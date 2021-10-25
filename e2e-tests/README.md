# Intro

This directory contains end-to-end tests for this application. These tests rely on an artifact stored in DNAstack's public github packages repo. At this point in time, retrieving a public github package still requires *any* github identity. Thus, in order to build this package, it is *required* that you have a [Github access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) with package read permissions [configured as a credential to our repository.](http://maven.apache.org/settings.html)

An example of such a configuration is provided below.


```xml
<repositories>
   <repository>
       <id>github-public</id>
       <name>Github DNAstack Maven Packages</name>
       <url>https://maven.pkg.github.com/DNAstack/dnastack-public-packages</url>
   </repository>
</repositories>
```

```xml
<servers>
   <server>
       <id>github-public</id>
       <username>$GITHUB_USERNAME</username>
       <password>$PERSONAL_ACCESS_TOKEN</password>
   </server>
</servers>
```
