# Root
### Requirements
 - Rooted Using [Magisk](https://github.com/topjohnwu/Magisk) Or [KernelSU](https://github.com/tiann/KernelSU).
 - [LSPosed](https://github.com/LSPosed/LSPosed) installed and fully functional.
 - A functioning arm64-v8a or armeabi-v7a device.

### Installation Guide

 1. Install the Module APK from either this [Github repo](https://github.com/rhunk/SnapEnhance/releases) or the [LSPosed repo](https://modules.lsposed.org/module/me.rhunk.snapenhance)
 2. Turn on the module in LSPosed and make sure Snapchat is in scope.
 3. Force Stop Snapchat.
 4. Open the menu by clicking the [Settings Gear Icon](https://i.imgur.com/2grm8li.png).
</details>

# Unrooted 
## Requirements
 - [LSPatch](https://github.com/LSPosed/LSPatch) installed and fully functional.
 - [Snapchat](https://play.google.com/store/apps/details?id=com.snapchat.android) installed and fully functional.
 - A functioning arm64-v8a or armeabi-v7a device.

> [!NOTE]
> Although LSPatch signifies that it requires Shizuku to function, this is not necessarily the case. If you wish to not use Shizuku, you may proceed with an alternative way by using [Split Apk Installer](https://github.com/Aefyr/SAI/).

## Preparations
Before Snapchat can be patched in accordance to SnapEnhance, you must download all the required files and store them in some place where you can easily manage and access them.<br/>
A file manager such as [MiXplorer](https://xdaforums.com/t/app-2-2-mixplorer-v6-x-released-fully-featured-file-manager.1523691/) can help with this.
> [!NOTE]
> File locations may vary depending on different factors.

Start off by downloading the Snapchat APK from your desired APK mirror website, we recommend [apkmirror](https://www.apkmirror.com/apk/snap-inc/snapchat/).

SnapEnhance offers two release branches; stable and debug, choose the one which you would like to install.

### Stable
This is the recommended way to use SnapEnhance as an unrooted user.<br/>
To get started simply head to our [release log](https://github.com/rhunk/SnapEnhance/releases/latest) and download the SnapEnhance APK depending on your device architecture and place it into a directory such as `/storage/emulated/0/SE`.

### Alternative: Debug
If you desire to experience the latest features which are not included in the stable releases yet, feel free to do so by downloading an Github Actions build from [here](https://github.com/rhunk/SnapEnhance/actions/workflows/debug.yml) instead of using a stable release build.

## Patching
If you are not familiar with how patching with LSPatch works this is for you.

First of all head into the App and open the `Manage` tab.<br>
Press on the Plus symbol and `Select the Snapchat APK from storage`.<br>
> [!NOTE]
> You could also use the installed Snapchat app, but users have reported issues with this before so it is not recommended doing so.

LSPatch offers a bunch of Patching options, the ones we are interested are:
 - Patch mode: `Integrated`
 - Signature bypass: `lv2`

Embed SnapEnhance as a module from storage (e.g. `/storage/emulated/0/SE/snapenhance_2.0.1-armv8-release-signed.apk`).<br>
After doing so press `Start Patch` and wait for it to finish.

Congratulations, you have successfully patched Snapchat!

> [!WARNING]
> GrapheneOS users might experience severe issues while patching normally, if you happen to use GrapheneOS please visit our [Troubleshooting Guide](https://github.com/rhunk/SnapEnhance/wiki/Troubleshooting#grapheneos).


## Installation
### LSPatch
If you have previously set up <u>LSPatch alongside with Shizuku</u>, you can safely install using the install option within LSPatch.

### Alternative: Split Apk Installer
If you have previously not set up Shizuku alongside LSPatch, LSPatch will throw an error upon trying to install Snapchat from within the app.

Open the Split Apk Installer app, press `Install Apks` and select all the APKs you have previously patched and install them.
