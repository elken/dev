:page/title Dropping Pods for Containers
:blog-post/author {:person/id :lkn}
:blog-post/created-at 2024-02-25
:blog-post/tags [:docker :nas :devops]
:blog-post/header-image /header/images/truenas-to-docker/preview.png
:open-graph/image /opengraph/truenas-to-docker.png
:open-graph/description An expedition into favouring simplicity
:blog-post/preview 
Kubernetes vs Docker; winner takes all.

:page/body 

I'll start with a little story about how I ended up in the position to write
this article. Feel free to skip if you're not interested in my motivations.

The criticisms and issues raised by me are my subjective experience. This isn't
meant to be a dump on TrueNAS, but I have documented my setup to provide some
context as to why I abandoned it and why I regret picking it in the first place.

My first foray into TrueNAS began way back when it was FreeNAS; a FreeBSD based
storage server OS. I'd always been a sideline admirer of the BSD ecosystem (and
still am, but sadly it's just a worse Linux when it comes to general usage) and
I had a project at my job at the time that FreeNAS was _perfect_ for. It's
freedom and powerful out-of-the-box functionality made it an easy sell, and I
was a big fan.

I left that job some time after for unrelated reasons (that server is still just
as alive now as it was then, it's nearly old enough to start secondary school)
and as such, FreeNAS fell to the wayside for me. I had little justification for
a home server back then and every job since was just software development.

Flash-forward to January of this year; and I decide it would be a fun side
project to build my own home server/NAS. My first choice distro was of course
FreeNAS, which I had learned some years ago had re-branded to TrueNAS, despite
briefly considering using a simple docker-compose based setup on top of
something like Fedora Server. I stuck with my first choice, remembering how well
everything fit together and how rock-solid it was.

TrueNAS for its applications uses a k3s (small kubernetes) setup to manage
things. I'll touch on this more in the critical section, but I will say if you
have really minimal needs you won't realise it's not docker. After adding
truecharts to get more packages, you can just point and click for the most part.

After getting things setup, I was met with a small hitch. The TrueNAS interface
isn't setup to handle SSL certificates without configuring a container called
"cert-manager" which in itself depending on a couple of others. After spending
an entire day to get these setup, all was fine for the most part.

Along with the certificates, I also wanted to setup traefik as a proxy to let me
resolve the containers by names rather than ports. This went painlessly for a
bit, until I realised that only charts that came from truecharts were able to be
configured to use traefik. No heads-up or indication of this anyway, so I had to
migrate any container I had setup on the default truenas charts to use the
truecharts version. And since you can't rename a container once it's deployed I
had to delete the old one first, which meant if there was no config backup
utility; I was SOL.

But at last; stability. The starter list of apps were up and green padlocked
with nice domain names. But everything changed when the ~~fire nation~~ DNS
attacked. See, k3s _relies_ on DNS to network everything; and the pihole setup I
was using was slightly buggy. And as a result of a slightly buggy DNS server, **all of my apps vanished**.

<figure>
    <img src="/images/truenas-to-docker/apps_gone.png">
    <figcaption>Figure 1. All my apps gone (this wasn't from installation)</figcaption>
</figure>

Yes, really. I didn't know this at the time of course, but panic set in straight
away. I was already aware that the interface didn't offer any way to backup the
container configurations, so before I could write everything down in an org
document (yes, _really_) I was back to square one.

Naturally, I was pissed. I'd spent many long hours getting everything just right
and poof. This also occurred at the same time that a
[poll](https://www.reddit.com/r/truenas/comments/1akgua4/container_technology_poll/)
was going on, with some of the replies implying that there was going to be a
wholesale migration from the k3s setup to a _much_ more sensible docker-compose
setup.[^1] 

These two things coupled was enough for me to investigate running a VM with all
my docker containers inside TrueNAS (the idea courtesy of
[rushsteve1](https://github.com/rushsteve1)). After some initial setup (which I'll go into more detail when I stop ranting, I will say at the time I was _pissed_), things seemed okay.

But the issue that broke the camel's back reared itself more and more. TrueNAS
from time to time would get "stuck" in the worst imaginable way. The whole
server would lock up but only as far as networking was concerned. I couldn't
access the TrueNAS interface nor could I SSH in. It was a coin toss if I could
access the containers within, so I kept putting it down to load. The physical
location made a hard restart very annoying, and I couldn't easily just plug a
monitor and a keyboard in to debug it.

But as I was moving the load from the host into the VM (which at the time was
using a fraction of the total system resources, so I could no longer attribute
it to load) I hit the issue to such a degree that even a restart wasn't fixing
it.

And so, TrueNAS was gone; and my second plan Fedora Server was in. Miraculously,
all my issues disappeared and setting up the containers was back to being
trivial.

I'm still recovering from this disaster attempt, but things are looking usable
again. Now onto something productive.

**Those looking to skip the story part can rejoice**

## A case for TrueNAS

So this isn't just a post about me dumping on TrueNAS, I will stick up for it a
little. The web UI is still quite polished, and does support a large amount of
what you're likely to want to do; and for everything else there's ~~mastercard~~
a shell you can connect to.

The ZFS management tools are unrivalled from what I have been able to find, even
[the best Cockpit plugin](https://github.com/45Drives/cockpit-zfs-manager)
doesn't match it. Anything you're likely to setup is made simple, and you have a
lot of flexibility with regards to complex things like RBAC and MAC.

When your application needs are small, you don't notice there's a k3s setup
there. It just feels like docker with some DNS applied over the top, and apps
are able to see each other nicely. The red/green deployment does seem to be
well-implemented (as far as I could tell, I didn't get much of a chance to
test...)

In short; if you _just_ want a storage server and something to maybe run Plex then this is a very solid choice.

However....

## A case against TrueNAS

As mentioned during the intro, while k3s is fine for simple cases it introduces
a lot of complexity for _very_ little benefit; if any. I personally maintain
that while Kubernetes and its ilk _definitely_ have a place; it solves **large**
scale problems. Problems that I can almost guarantee nobody using a free
operating system to power a storage server will ever hit. You don't even get the
usual benefit of Kubernetes in that you're usually able to connect multiple
nodes to a single cluster. Each box is 1 node in its own cluster. Machine goes
down? SOL (don't I know that...)

Minor gripes about the interface:
- VM settings don't seem to apply. Any changes I made through the interface
  didn't take, I had to connect to libvirt on the server from my desktop in
  order to apply them. There's also a severe lack of options.
- It tries to be helpful sometimes with quick-fix suggestions, but there were a
  lot of very simple cases missed. The DNS issue RE: Kubernetes is a pretty
  severe one, DNS not being able to resolve should **NOT** mean that because
  none of the applications were resolvable, they didn't exist. Down and
  non-existent are two different states.
- The missing link between custom ingress and built-in charts is not made clear.
- **The fact you have to setup 3 pods just to have Traefik not use the default
  SSL certificate**. I had one setup fine on the host machine, but there's just
  no option to specify it. And no way to tweak the charts or settings.
- **The fact there's no way to backup app configurations**. Yes, the data
  themselves end up in a pool you can restore from (a life-saver that I haven't
  lost my Mealie changes) but the actual settings you setup the containers with
  like ingress, certificates, paths and the like are transient. I found no
  references to them in the exported settings zip, but maybe I'm wrong there.
  
## Setting up your containers in a VM

Obviously I have a bias against using it at all due to the issues I've had, but
I do still maintain that it has a place. If you're really sold on the idea of
using TrueNAS as a storage server and putting everything inside a single or
multiple VMs; here's some further setup and pointers.

Firstly, to make things easier I strongly recommend setting up virt-manager on
your own device(s) and connecting to the VMs that way. The web serial
console/display isn't perfect, and you get access to a lot more customisations.

Assuming a default configuration, you can connect by going into virt-manager and
doing File -> Add Connection. In this box, select "Custom URI" as the hypervisor
and amend the below to reflect your setup.

```shell {.no-line-numbers .command-line}
qemu+ssh://admin@<ip>/system?socket=/run/truenas_libvirt/libvirt-sock
```

This `admin` user (or whichever you wish to use) must be in the `libvirt` group
and possibly the `adm` group (I didn't get a chance to test _this_ either...).
Connect and that's it.

You can do whatever you want, connect drives (remember that the paths should be
relative to the _host_ machine not yours), connect other displays, etc.

Next up, because the TrueNAS host is managing the drives as a pool; you can't
connect them directly to the VM. The best approach you have here is setting up
NFS shares for all the datasets you want to share, ensuring that you do indeed
do them for every dataset. There's no recursion here, you must specify every
nested dataset you want to share.

Then it's a simple case of mounting them on the VM with

```shell {.no-line-numbers .command-line}
sudo mount -t nfs <ip>:/path/to/share /local/path
```

Taking care to ensure that either `MAPROOT` or `MAPALL` user/group is setup to
be someone that has access to everything. I used `root`for testing, and due to
me wiping this setup some time after I didn't get further.

If you want to go down this route, I make no recommendation either way. You know
your needs.

## A docker-compose based setup

So; as mentioned before I have settled now on a docker-compose setup on Fedora Server. The distribution doesn't matter here, the tools mentioned should run on any modern mainstream server OS.

[Cockpit](https://cockpit-project.org/) is with little competition the _best_
server management interface I've used. The little quick-fixes that TrueNAS tries
to do pale in comparison to what Cockpit offers. Without even opening a
terminal, I had setup the networking, installed a few more plugins and got
[portainer](https://www.portainer.io/) up and running.

For those who want portainer to start up on boot, a good solution that `podman` offers is called "quadlets". These let you manage containers via SystemD, in short.

Create the following file

`/etc/containers/systemd/portainer.container`
```systemd
[Unit]
Description=Interface for managing containers
After=local-fs.target

[Container]
# I use this image to remove some of the prompts about business features
Image=docker.io/ngxson/portainer-ce-without-annoying:latest
PublishPort=9443:9443
Volume=/run/podman/podman.sock:/var/run/docker.sock:Z
Volume=portainer_data:/data:rw,Z

[Service]
Restart=always

[Install]
# Start by default on boot
WantedBy=multi-user.target default.target
```

And run the following commands:

```shell {.command-line data-user="root"}
systemctl daemon-reload
systemctl start portainer
```

And thanks to `Restart=always`; this service will also run on boot. Don't try
and run `systemctl enable` with it, it won't work.

A bit of copy-and-pasting from linuxserver and I was in a few hours almost back
to what had previously taken me a couple of days to get right. Going into it I
was already familiar with the setup I wanted, so this wasn't a case of gaining
knowledge; it's just much easier to setup containers with docker-compose.

I don't have to worry about setting up ingress or anything, or even knowing
about it. Adding traefik was as simple as adding another container to the stack
and adding some labels **and a quick change to remove the default certificate**.

I also have backups in place for all the important data that I've verified; so I
can go from a blank server to setup in a few steps.

## Closing

One could argue that this is a lesson to take regular backups, and I could
counter that with TrueNAS' lacking ability with regards to application
configuration. I hope the project does move forward with dropping k3s for
docker, and that it offers a facility to backup and restore these stacks.

Going forward it's the best decision for the project, but it doesn't impact me
in the slightest now. All the issues that plagued my TrueNAS setup are gone, all
my containers have direct access to the ZFS pool, no performance issues. I can
access Cockpit, Portainer and all my containers all the time even when the host
is at 100% CPU usage.

For those that _really_ skimmed to end up here; TrueNAS has its place as a
simple storage server you intend to host a couple of applications on. I wouldn't
recommend using it as an entire home server distribution.

[^1]: For the sake of clarity, none of the replies at the time directly stated this, it was simply my interpretation of some of the replies from people at iX.
