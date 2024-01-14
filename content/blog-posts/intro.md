:page/title Everyone's first blog post is about how they made their blog
:blog-post/author {:person/id :lkn}
:blog-post/created-at 2023-12-10
:blog-post/tags [:powerpack :meta]
:blog-post/header-image /header/images/intro_preview.png
:open-graph/image /opengraph/intro.png
:open-graph/description Blogging is fun; and why yes the year _is_ 2007
:blog-post/preview

This site has been _long_ overdue for a clean refactor. I spent so long chasing the "perfect" generator; and it seems I've finally found it.

:page/body

 Initially this was a post about quickblog, and you can still find the original at the bottom; but I've now migrated to [powerpack](https://github.com/cjohansen/powerpack) which wraps around a number of Ring technologies to provide a _wonderful_ setup for producing static sites.

So why has powerpack, a recently released static-site generator, been the one to solve all my problems?

Well, that's a _teeny_ lie because it doesn't handle org-mode but I have settled for not requiring it. As for the rest, a list I'm happy to settle on is:

- Live reloading
- Configurable asset pipeline
- A more advanced format than plain markdown that cleanly handles metadata and other things
- Some way to "query" other pages like Gatsby (the framework is overloaded to all hell but the GraphQL API is so nice)
- Views are Hiccup
- As much of the project in Clojure so I can easily tap into the ecosystem I'm most familiar with these days
- Leaning into the last point a bit; REPL support

So let's quickly tackle them one at a time!

## Live reloading

Okay this one doesn't need much explanation right? 

You save a file, the page reloads. Next! Well, I also want this for things like views, config changes etc.

Powerpack supports live reloading for everything, if you make a change that requires a full restart/reload it'll take care of that too. I have had times where I've had to manually restart from the REPL but that's usually been because of an exception elsewhere not bubbling correctly.

## Asset pipeline

Not _really_ super useful for a blog like this, but I also wanted something I could use for other similar projects.

Turns out a couple of the libraries Powerpack brings in are [Optimus](https://github.com/magnars/optimus) and [Imagine](https://github.com/cjohansen/imagine) which give me asset optimization (short version is in dev mode everything is just files, in prod mode they get bundled and minified) and image manipulation (short version is you define a transformation to be applied to images and you can refer to that dynamically for any image by calling the url `/transformation-name/path-to-image`) and this results in a lot of power.

## Nicer than Markdown format

Markdown is _fine_. It gets ragged on a lot, for fair reason in some cases, but for what it is it does the job just fine. What it does lack however is good metadata and structural definition outside of headings.

Powerpack supports [mapdown](https://github.com/magnars/mapdown) which is a superset of markdown that adds in edn support.

A file like

```markdown
​:title Mapdown example
​:author Magnar Sveen
​:body

Here's an example of how mapdown works.

It's, like, text with keywords.

​:aside

There's not much to it, really.
```

Is translated to

```clojure
(ns example.core
  (:require [mapdown.core :as mapdown]))

(mapdown/parse (slurp "intro.md"))

;; =>
{:title "Mapdown example"
 :author "Magnar Sveen"
 :body "Here's an example of how mapdown works.\n\nIt's, like, text with keywords."
 :aside "There's not much to it, really."}
```

So we get metadata built in for free! No longer do we have to rely on awkward parsing or hunting for markers; if you want to add a preview you just add a field (and some data in the schema, more on that soon....)

## "Page API"

I couldn't think of a nice heading for this, but the point boils down to some way to query data from all the other pages in the system. Things like tag pages, page listings, sitemaps, feeds etc are much easier to generate in this way and it makes components much more flexible when they can take into consideration the context of both the current page and all other pages.

Powerback backs this with [Datomic](https://datomic.com/), with all the pages and data files transacted into this in-memory DB. Creating a new page is as simple as creating a mapdown file with a `:page/uri` field; which also includes support for returning other kinds of data (if you desire a JSON response then append `.json` to the URI for example). 

Adding in other fields simply requires you to adjust the `resources/schema.edn` file. Once done, you can use the new defined fields in any pages and you can even namespace them to separate "kinds" of pages. You also then get a degree of "type safety" (now my blog has the new buzzword) with pages of the same "kind" warning you that some data is missing.

## Hiccup views

The biggest flaw of Quickblog and similarly designed static-site generators (not a dig at quickblog, it's built for a different use case; what it does it does _very_ well) is the idea of everything being done through a HTML templating engine like Jinja or Jekyll or I'm not going to list any more you get my gist.

These are inferior to the React-based generators because of the concept of components vs views. A lot of these template engines support things like passing arguments, but because they're still HTML everything just ends up being a string and it gets _*extremely*_ messy.

But for _components_; they're just functions as the language expects so arguments work the same as any other function. No awkward constructs to handle conditional logic or looping or anything; you just use normal language constructs and return HTML. 

Where Hiccup takes a big win here is the HTML you end up returning is *just a vector*. If anyone's ever done manual HTML generation like this; I sympathise with you. JSX bridges the gap quite well, but it's fraught with its own complexity issues. With Hiccup though you never have to think about syntax, you just return a vector with you component. If you need to return more than one, you don't need to use `<>` Fragments, you just return a vector of vectors. Lisps really are perfect for writing HTML, even in libraries like uix where there's no hiccup, the `($ :div "hello")` style is still preferential to writing HTML directly, but I digress.

Hiccup good, HTML bad. Composition good.

## Clojure ecosystem

As this is where I spend most of my working life with, I've become quite accustomed. I know the libraries I need for things like [icons](https://github.com/cjohansen/phosphor-clj), [Component showcases](https://github.com/cjohansen/portfolio) and the like (the observant reader will spot that the same author wrote both of those as well as powerpack, shout-outs to [Christian Johansen](https://github.com/cjohansen)), I understand how Ring works and most importantly I understand the language.

As nice as Hugo is, the thought of writing Go has me up at night and I just couldn't commit to it. 

The triviality in which I can navigate around a Clojure project (due to the wonderful [language server](https://clojure-lsp.io/)) makes it really simple to understand exactly what a function does and peek at the implementation without leaving my editor. I won't turn this into a "Clojure is great" post, that might come later but the point of this one is being able to lean into an ecosystem I'm comfortable in already.

## REPL

And last but certainly not least; the REPL.

Nicely supporting all the previous points, the REPL underlines everything and gives me a powerful window into my site. At any given moment, I can run a Datomic query against my site to test things or get an idea of the current state, I can lean into data viewers with something like [portal](https://github.com/djblue/portal) or just inspect them directly in the REPL. I can quickly prototype a complex component, though with live reloading it's often quicker to just use that.

I can also manage the Powerpack server through it, no need to jump to a terminal buffer and mash `C-c` until it gives in. Just a simple `(dev/reset)`and everything is back up (sometimes I have to `(dev/stop)` and `(dev/start)` as mentioned before, but that's normally an ugly exception ), I could also write my own helper `dev` functions to get loaded whenever I start my REPL.

## Closing

In short, Powerpack ticks all my boxes. I was actually in the midst of making my own based around uix server-side rendering and lo and behold 2 days later Powerpack released. As all good Clojure projects are, it [builds on the shoulders of other great libraries](https://github.com/cjohansen/powerpack#whats-in-the-box) rather than reinventing, which is probably my favourite trope of the Clojure world.

What follows now is the original article about quickblog. Just because I moved away from it doesn't mean it's not a valuable or inferior project, and I encourage you to also check it out.

# Quickblog 

So; what is quickblog I hear you shouting? (No really, I've hooked up the MediaStream Recording API; try saying something[^1])

Well it's one of [borkdude](https://github.com/borkdude "borkdude github")'s many wonderful packages; this time around it's a very simple static generator. As the name implies, the emphasis is on ***quick***-ly spinning up a ***blog*** and running with it. It's a very minimal core, as seen below thanks to [scc](https://github.com/boyter/scc "scc github")

```
────────────────────────────────────────────────────────────────────────────
Language              Files     Lines   Blanks  Comments     Code Complexity
────────────────────────────────────────────────────────────────────────────
Clojure                   3      1251      133        32     1086         90
────────────────────────────────────────────────────────────────────────────
Total                     3      1251      133        32     1086         90
────────────────────────────────────────────────────────────────────────────
```

Which also includes a default configuration and all the `bb` tasks. _Not bad at all_.

As such, it's a very hackable core. Since I've started using it, I've already contributed a number of fixes upstream.

By default, markdown posts live in `posts/` and the few supported template constructs go in `templates/`. You may also provide content via `assets/` which get copied to `templates`. Out of the box, there is autogenerated templates for an index page of blog posts, a post page, an overall page template, an archive listing page and a list of all tags.

Handled either by adding a `:about-link` config key or providing an `about.html` template, you can also provide an additional about page. All of these templates are editable, and as long as you're editing in watch mode you should see your changes near-instantly.

```bash {.command-line}
bb quickblog watch
```

Also included is support for an atom feed; which with the advent of people moving to RSS readers is quite welcomed.

Thanks to [Anders Eknert](https://www.eknert.com/blog/quickblog), I also have a lovely copy-pastable github action (you should also go read his post)

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: delaguardo/setup-clojure@12.1
        with:
          bb: latest
      - run: bb quickblog render
      - uses: actions/upload-pages-artifact@v2
        with:
          path: public/
  deploy:
    needs: build
    permissions:
      pages: write
      id-token: write
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/deploy-pages@v2
        id: deployment
```

[^1]: I haven't really
