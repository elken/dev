:page/title Writing efficient Emacs modeline segments
:blog-post/author {:person/id :lkn}
:blog-post/created-at 2025-07-29
:blog-post/tags [:emacs :editor :lisp]
:blog-post/header-image /header/images/writing-efficient-emacs-modeline-segments/preview.png
:open-graph/image /opengraph/writing-efficient-emacs-modeline-segments.png
:open-graph/description Build your own powerful modeline segments!
:blog-post/preview
Writing complex segments can seem performance intensive; but we can work on that
:page/body

Your editor's status bar could be telling you everything you need to
know, or it could be telling you nothing useful at all.

<div class="flex items-center gap-4">
    <figure>
        <img src="/images/writing-efficient-emacs-modeline-segments/nvim_setup.jpg" alt="A typical Neovim setup" class="h-96" />
        <figcaption>A typical Neovim setup (<a href="https://www.devas.life/effective-neovim-setup-for-web-development-towards-2024">credit</a>)</figcaption>
    </figure>
    <figure>
        <img src="/images/writing-efficient-emacs-modeline-segments/emacs_setup.png" alt="A typical Emacs setup" class="h-96" />
        <figcaption>A typical Emacs setup (<a href="https://www.curiouslychase.com/posts/getting-started-with-emacs">credit</a>)</figcaption>
    </figure>
</div>

This contrast captures a fundamental difference in philosophy. One
approach says "show everything always"—file trees, git status,
diagnostics, system info, all fighting for attention whether you need
them or not. The other says "show what matters when it matters."

I'm firmly in the second camp. When I'm reading a PDF, I want page
numbers—not git blame info. When I'm debugging a deployment, I want CI
status—not my music player. When I'm deep in a coding flow, I want
just enough information to stay oriented without visual noise breaking
my concentration.

This is where Emacs truly shines. While other editors bolt on features
through plugins that may or may not cooperate, Emacs gives you the
primitives to build exactly the interface you want. Need to see
deployment status while working on a hotfix? Trivial. Want weather
info when deciding whether to bike to work? Built in minutes. Current
song while you code? Why not.

The magic happens in the modeline—that thin strip at the bottom that
most people ignore. In Emacs, it becomes a contextual dashboard that
adapts to what you're actually doing. But here's the catch: building
these powerful, live-updating segments naively will bring your editor
to its knees...

# Building a simple modeline segment

Let's start by building the "wrong" way first—a naive implementation that
demonstrates exactly why this is tricky. We'll create a simple weather
widget using wttr.in that updates live in your modeline.

For simplicity, we'll just worry about the one-line output version.

```emacs-lisp
;; Define a variable that mode-line-format will call
;; :eval means evaluate the form and return the value in-place
(setq-default my/wtter-status
  '(:eval
    (when (mode-line-window-selected-p)
      (propertize  (with-current-buffer (url-retrieve-synchronously "https://wttr.in/London?format=3")
                    (set-buffer-multibyte t)
                    (goto-char (point-min))
                    (re-search-forward "^$" nil 'move)
                    (forward-char 1)
                    (string-trim-right
                     (decode-coding-string
                      (buffer-substring-no-properties (point) (point-max))
                      'utf-8)))
                  'face 'doom-modeline))))

;; Needed so Emacs knows it can evaluate it safely
(put 'my/wtter-status 'risky-local-variable t)

;; Default modeline (evaluate this to fix)
(setq-default mode-line-format
              '("%e" mode-line-front-space
                (:propertize
                 ("" mode-line-mule-info mode-line-client mode-line-modified
                  mode-line-remote)
                 display (min-width (5.0)))
                mode-line-frame-identification mode-line-buffer-identification "   "
                mode-line-position (vc-mode vc-mode) "  " mode-line-modes
                mode-line-misc-info mode-line-end-spaces))

;; Our custom one
(setq-default mode-line-format
              '("%e" mode-line-front-space
                (:propertize
                 ("" mode-line-mule-info mode-line-client mode-line-modified
                  mode-line-remote)
                 display (min-width (5.0)))
                mode-line-frame-identification mode-line-buffer-identification "   "
                mode-line-position (vc-mode vc-mode) "  " mode-line-modes
                mode-line-misc-info my/wttr-status mode-line-end-spaces))

;; If you have issues with the modeline now showing the new status
(kill-local-variable 'mode-line-format)
```


Great! Job done, how easy!?

Well ... not quite. If you tried to evaluate that blindly without
reading the slightly spoiler-y comments, this brings Emacs to its
knees. Every time the modeline wants to update, it's calling out to
`wttr.in`. There's a lot of triggers that cause the modeline to
update, so you likely had constant pauses and freezes.

> But you promised we can make them fast?

I did! The problem is clear: we're making synchronous network calls every
time Emacs wants to refresh the display. That's like stopping to call
your friend for the weather update every time you glance at your phone.

So let's see what we can do.

# Making things better

Okay so right away you should be able to spot the issue, we're calling
synchronously to a service and blocking every time.

For some things, this can be ideal for example if you need the result
and you won't do any other operations in the meantime. But unless you
want to use Emacs as an interface to `wttr.in`, you'll probably want
to use it for other things too. So thanks to the async nature of
`url-retrieve`, we can shift the burden of this processing to a thread
and shift the current model of how we handle this.

Let's get into the weeds a bit here

## Pub/Sub

So what we're doing currently works just fine for simple data, that's
how the default Emacs modeline segments work. I encourage you to check
them out, all the symbols in the above `mode-line-format` variable are
variables themselves, so have a look in `C-h v mode-line-`. Most of
the time we're just taking the value of other variables and formatting
them nicely.

However when there's complex processing or even an external service to
consider, we have to change the model from "run this function
constantly" to instead do "format this variable constantly". That
would be ideal, wouldn't it?

A partial solution we could do here is using a future, we could lean
on that part of `emacs-async` and have the processing work like
that. That'll work right?

Well ... not quite. The problem is that the modeline still calls our
function constantly—it's just that now each call creates a new future.
So instead of one blocking call, you get dozens of pending network
requests stacking up, as the diagram below demonstrates.

![Unresolved futures diagram](/images/writing-efficient-emacs-modeline-segments/futures_gantt.svg "Futures diagram")

As time goes on, more futures are being created but they don't ever
properly resolve.

Instead of "run this code every time the modeline refreshes," we want
"update this variable occasionally, and let the modeline just display it."
Think of it like the difference between asking someone the time every few
seconds versus glancing at a clock that updates itself.

This is where the idea of pub/sub comes in.

Short for "publisher/subscriber", it describes a relationship between
two services (in the sense of long-running processes) in which one
pushes data out and one or many other services listen out for
updates.

Think of it like a newspaper delivery model: instead of you walking to
the newsstand every time you want to check headlines (the blocking
approach), the newspaper gets delivered to your door once a day and
you just read whatever's there when you want it (the pub/sub
approach). The "publisher" (weather service) does its work on its own
schedule, and the "subscriber" (your modeline) just displays whatever
the latest data is.

Thanks to the asynchronous bits of `url-retrieve`, we can perform a crude
implementation of this[^1].

Here's how it works in practice: we'll set up a background timer that
fetches weather data every few seconds and stores it in a simple variable.
The modeline will just display that variable's contents—no network calls,
no blocking, just fast text formatting.

Let's build it.

```emacs-lisp
;; Define this first to appease the byte-compiler gods
(setq-default my/wttr-status-text "")
(put 'my/wttr-status-text 'risky-local-variable t)

;; Define an async callback with url-retrieve
(defun my/wttr-update ()
  "Update the current wttr status asynchronously."
  (message "Updating wttr.in")
  (let ((url-show-status nil))
    (url-retrieve "https://wttr.in/London?format=%l:%20%C+%t&q"
                  (lambda (status)
                    (if (plist-get status :error)
                        (message "Failed to fetch weather: %s" (plist-get status :error))
                      (progn
                        (set-buffer-multibyte t)
                        (goto-char (point-min))
                        (re-search-forward "^$" nil 'move)
                        (forward-char 1)
                        (setq my/wttr-status-text
                              (string-trim-right
                               (decode-coding-string
                                (buffer-substring-no-properties (point) (point-max))
                                'utf-8)))
                        (force-mode-line-update)
                        (message "Weather updated: %s" my/wttr-status-text)))
                    (kill-buffer)))))

;; Define a global timer we can start/stop
(defvar my/wttr--timer nil)
(defun my/wttr-timer ()
  "Start/stop the timer to update wttr status."
  (if (timerp my/wttr--timer)
      (cancel-timer my/wttr--timer)
    (setq my/wttr--timer
          (run-with-timer
           1                 ;; Delay for the first run
           5                 ;; How often in seconds to run after the first
           #'my/wttr-update ;; The function to call
           ))))

;; Our custom modeline using our new text variable
(setq-default mode-line-format
              '("%e" mode-line-front-space
                (:propertize
                 ("" mode-line-mule-info mode-line-client mode-line-modified
                  mode-line-remote)
                 display (min-width (5.0)))
                mode-line-frame-identification mode-line-buffer-identification "   "
                mode-line-position (vc-mode vc-mode) "  " mode-line-modes
                mode-line-misc-info my/wttr-status-text mode-line-end-spaces))

;; If you have issues with the modeline now showing the new status
(kill-local-variable 'mode-line-format)
```

_Much_ nicer! I've added an annoying "Updating" message so you can see
how often it's really updating (spoiler alert: often) and yet, no
impact on the editor!

`url-retrieve` runs the request asynchronously and upon getting the
result, will run the specified callback on the buffer. The buffer we
get back has the entire response including HTTP headers, which is why
we have to skip to the first blank line to get the response. Here all
our callback does is simply set the value, trigger a modeline update
to ensure that it is always up to date then print another message to
say it's done (this is purely for our demo purposes).

We supplement this with a simple timer to call our update class every
5 seconds and an easy way to disable the timer on the fly.

# Where to go from here

This pattern—background updates with variable binding—works for any external
data source you want in your modeline. CI/CD status, music players, system
monitoring, you name it.

## Tools and Extensions
- I use `doom-modeline` myself, which has a nice API for defining segments: https://github.com/seagle0128/doom-modeline
- For split-style modelines, check out `mode-line-format-right-align`
- Here's a real-world example using this pattern: https://github.com/elken/doom-modeline-now-playing (you can find the old implementation [here](https://github.com/elken/doom-modeline-now-playing/blob/1532f324f98a234aa14e12ebdfd17cebba978d6a/doom-modeline-now-playing.el#L110-L129))
- For more complex needs, there is
  [`emacs-async`](https://github.com/jwiegley/emacs-async) which spins
  up an instance of Emacs that can use around 60-100MB of RAM, so this
  isn't advised for very simple operations like this that have
  alternatives[^2]

## Further Reading
- For more comprehensive modeline customization, see [Prot's excellent tutorial](https://protesilaos.com/codelog/2023-07-29-emacs-custom-modeline-tutorial/)

The key insight is simple: let background processes do the heavy lifting,
and keep your modeline fast by just formatting pre-computed data.

If anyone finds any other useful materials, please do leave them in the comments, and have a great day!

[^1]: Crude in the interest of time/code here. We could absolutely build something to implement this but it's out of scope of this post.

[^2]: Thanks to [`@karthink`](https://karthinks.com/) for this
