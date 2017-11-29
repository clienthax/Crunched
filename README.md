# Crunched
A Java based Crunchyroll downloader with full subtitle and season support.

## Prerequisites
* Java
* FFMpeg + FFProbe (https://www.ffmpeg.org/download.html)
* Mkvmerge (https://mkvtoolnix.download/downloads.html)

## Building
gradlew fatJar

## Basic usage
java -jar crunched.jar -email crunchyrollemail -pass password -page http://crunchyroll.com/#######

## Command-line Interface (`java -jar Crunched.jar`)

The [command-line interface](http://en.wikipedia.org/wiki/Command-line_interface) does not have a graphical component and is ideal for automation purposes and headless machines. The `java -jar Crunched.jar` command will produce the following output:

    Usage: java -jar Crunched.jar [options]

    Options:
      -h, --help         output usage information
      -u, --user <s>     The e-mail address.
      -p, --pass <s>     The password.
      -page, --page <s>  Crunchyroll series page
      -ffmpeg <s>          Path to ffmpeg
      -ffprobe <s>         Path to ffprobe
      -mkvmerge <s>        Path to mkvmerge
      -addtitle            Add the episode title to the filename
      -socksproxy <s>      Socks proxy to use for login and api requests ONLY, (This will not be used for downloading as only the api is geolocked), eg 127.0.0.1:9999
      -httpproxy <s>       Http proxy to use for login and api requests ONLY, (This will not be used for downloading as only the api is geolocked), eg 127.0.0.1:9999
      
## Legal Warning

This application is not endorsed or affliated with *CrunchyRoll*. The usage of this application enables episodes to be downloaded for offline convenience which may be forbidden by law in your country. Usage of this application may also cause a violation of the agreed *Terms of Service* between you and the stream provider. A tool is not responsible for your actions; please make an informed decision prior to using this application.

**PLEASE _ONLY_ USE THIS TOOL IF YOU HAVE A _PREMIUM ACCOUNT_**

#### Credits
Godzil for the original tool that I have used for a long while, and which gave me the idea to make my own tool

Crunchyroll for being an amazing service in the first place
