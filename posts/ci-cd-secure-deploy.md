Title: Securely deploying to a server using CI/CD
Date: 2020-02-28
Tags: ci/cd,devops
Image: assets/2020-02-28/preview.jpg
Image-Alt: Group of computer servers

Been a while since I've posted anything (work, etc) so I thought I'd post about what I recently did to publish this very site! 

<div class="warning">
Note that since this post, I have migrated away from Gatsby in favour of [quickblog](https://github.com/borkdude/quickblog "quickblog") for simplicity's sake; but the instructions here still apply for the most part.
</div>

<!-- end-of-preview -->

Below is the default template for building Gatsby sites.

```yaml
image: node:latest

cache:
  paths:
    - node_modules/

pages:
  script:
    - yarn install
    - ./node_modules/.bin/gatsby build --prefix-paths
  artifacts:
    paths:
      - public
  only:
    - master
```

_Obviously_ this doesn't handle any kind of deployment so we're going to have to add that ourselves. Because I'm not an idiot (although this is debatable), I'll be using variables for my information (server IP & private key).

## Generate SSH key

If you want to generate a new SSH key (recommended, reduces the fallout of having a key compromised), execute the below commands

```bash command-line no-line-numbers
ssh-keygen -f /home/$USER/.ssh/gitlab-deploy
```

```
Generating public/private rsa key pair.
Enter passphrase (empty for no passphrase):
Enter same passphrase again:
Your identification has been saved in /home/elken/.ssh/gitlab-deploy.
Your public key has been saved in /home/elken/.ssh/gitlab-deploy.pub.
The key fingerprint is:
SHA256:xyeTjhvNQLLAmuRFZFW91ay5/Fz0hFMbipcqvB1klNc elken@zenith
The key's randomart image is:
+---[RSA 2048]----+
|   .+.....  .o.  |
|   +      .o..oE.|
|  . + . . .oooooo|
| o + . + ..=o+o.o|
|  +   . S O.+. +.|
|         X *o   o|
|        o B .o . |
|         + .  o  |
|        .        |
+----[SHA256]-----+
```

You should ideally set a passphrase for this key to _further_ reduce the fallout of having the key compromised.

## Set the variables

Navigate to your project's settings, then to the CI/CD section in the sidebar and finally expand Variables to be presented with an emptier version of the below.

![Gitlab config](/assets/2020-02-28/lab-conf.png "Gitlab configuration")

Copy the structure for the above variables, inputting your own values for the IP address of the server and the body of the SSH private key (the one you created earlier, in my example `/home/$USER/.ssh/gitlab-deploy`).

## .gitlab-ci.yml

The final step is to configure our `yml` file to connect and deploy to our server. Using the above config, the below shows a diff to get to the intended configuration.

```diff-yaml diff-highlight
@@ -1,16 +1,24 @@
 image: node:latest

 cache:
+  key: "$CI_JOB_NAME-$CI_COMMIT_REF_SLUG"
   paths:
     - node_modules/

+before_script:
+  - mkdir -p ~/.ssh
+  - echo "$SSH_PRIVATE_KEY" | tr -d '\r' > ~/.ssh/id_rsa
+  - chmod 600 ~/.ssh/id_rsa
+  - eval "$(ssh-agent -s)"
+  - ssh-add ~/.ssh/id_rsa
+  - echo $(ssh-keyscan $IP_ADDRESS 2> /dev/null) >> ~/.ssh/known_hosts

 pages:
   script:
     - yarn install
     - ./node_modules/.bin/gatsby build --prefix-paths
+    - scp -r public/* root@$IP_ADDRESS:/home/user-data/www/elken.dev
   artifacts:
     paths:
       - public
```

Using the variables we declared earlier, we are able to deploy to the intended server and path we desire!
