[TheVisual](https://github.com/TheVisual) commented on Jan 29
--
If you're using a custom rom/kernel go back to stock they hate custom roms

[RSR1337](https://github.com/RSR1337) commented on Jan 29
--
>If you're using a custom rom/kernel go back to stock they hate custom roms

True 👍

[DjCrqss](https://github.com/DjCrqss) commented on Jan 29
--
+1, using the latest APK and LsPatch unrooted method. Previous APK and SnapEnhance 2.0.5 worked fine.
Update: using the latest LsPatch and the SnapChat 12.69.0.26 APk worked perfectly for me. New Snapchat may have more security. [ApkMirror link](https://www.apkmirror.com/apk/snap-inc/snapchat/snapchat-12-69-0-26-release/snapchat-12-69-0-26-2-android-apk-download/download/?key=8ee5556646684847168072c11a43963dc35dcb4a&forcebaseapk=true)

[Anshu-Bijarnia](https://github.com/Anshu-Bijarnia) commented on Jan 29
--
>  +1, using the latest APK and LsPatch unrooted method. Previous APK and SnapEnhance 2.0.5 worked fine.
> Update: using the latest LsPatch and the SnapChat 12.69.0.26 APk worked perfectly for me. New Snapchat may have more security. [ApkMirror link](https://www.apkmirror.com/apk/snap-inc/snapchat/snapchat-12-69-0-26-release/snapchat-12-69-0-26-2-android-apk-download/download/?key=8ee5556646684847168072c11a43963dc35dcb4a&forcebaseapk=true)

Were you using stock rom or custom?

[DjCrqss](https://github.com/DjCrqss) commented on Jan 29
--
> >  +1, using the latest APK and LsPatch unrooted method. Previous APK and SnapEnhance 2.0.5 worked fine.
> > Update: using the latest LsPatch and the SnapChat 12.69.0.26 APk worked perfectly for me. New Snapchat may have more security. [ApkMirror link](https://www.apkmirror.com/apk/snap-inc/snapchat/snapchat-12-69-0-26-release/snapchat-12-69-0-26-2-android-apk-download/download/?key=8ee5556646684847168072c11a43963dc35dcb4a&forcebaseapk=true)
>
> Were you using stock rom or custom?

Stock Samsung OneUI 5.1 ROM on android 13

[RevealedSoulEven](https://github.com/RevealedSoulEven) commented on Jan 30
--
Even that version is not working too for custom roms

[Not-Smelly-Garbage](https://github.com/Not-Smelly-Garbage) commented on Jan 30
--
Back up your config by exporting it through the snapenhance app, then toggle everything off and log in. It should work, then you can import all your previous settings and be good

[RevealedSoulEven](https://github.com/RevealedSoulEven) commented on Jan 31
--
> Back up your config by exporting it through the snapenhance app, then toggle everything off and log in. It should work, then you can import all your previous settings and be good

Even uninstalled it. Still the same

[histefanhere](https://github.com/histefanhere) commented on Jan 31 
--
Any snapchat version, no patches, added sc to magisk denylist, hidden apps via hide my applist and still doesn't allow login. It's possible this might not be a SnapEnhance issue but this is nonetheless a good place to discuss this.

[RevealedSoulEven](https://github.com/RevealedSoulEven) commented on Feb 2
--
> Any snapchat version, no patches, added sc to magisk denylist, hidden apps via hide my applist and still doesn't allow login. It's possible this might not be a SnapEnhance issue but this is nonetheless a good place to discuss this.

Actually, it's a custom rom check, where your device fingerprints and play integrity is checked. Even if you bypass play integrity it won't work coz it needs even Strong Play Integrity to be true

[TheVisual](https://github.com/TheVisual) commented on Feb 2
--
> >  Any snapchat version, no patches, added sc to magisk denylist, hidden apps via hide my applist and still doesn't allow login. It's possible this might not be a SnapEnhance issue but this is nonetheless a good place to discuss this.
> 
> 
> Actually, it's a custom rom check, where your device fingerprints and play integrity is checked. Even if you bypass play integrity it won't work coz it needs even Strong Play Integrity to be true

Custom roms no longer work people need to go back to stock also no Strong Play Integrity is not enforced only Key Attestation / Play Integrity Basic (Standard Aswell) also some phones are banned especially older that are no longer widely used on snapchat as they probably fell victim to abuse.

[Blurry-face-99](https://github.com/Blurry-face-99) commented on Feb 2
--
I don't understand why the support from the Snapchat side is zero except an automated email. Also, did the issue got resolved yet?

[karimmec999](https://github.com/karimmec999) commented on Feb 3
--
Can i bypass custom rom check by using and spoofing device informations (like changing build.prop, spoofing imei, device id ...etc)

[fluid46](https://github.com/fluid46) commented on Feb 5
--
Also having this issue, using version suggested, latest LSPatch and stock ROM, unrooted android.

[TheVisual](https://github.com/TheVisual) commented on Feb 5
--
> Can i bypass custom rom check by using and spoofing device informations (like changing build.prop, spoofing imei, device id ...etc)

You will bootloop so no maybe hooking possible

>  I don't understand why the support from the Snapchat side is zero except an automated email. Also, did the issue got resolved yet?

if you think there gunna help you for using a mod lol nah its against TOS

[karimmec999](https://github.com/karimmec999) commented on Feb 5
--
> >  Can i bypass custom rom check by using and spoofing device informations (like changing build.prop, spoofing imei, device id ...etc)
> 
> 
>  You will bootloop so no maybe hooking possible
> 
don't worry about boot looping because i tried doing all what i mentioned above....so any possible would be helpful Thanks.

[Verequies](https://github.com/Verequies) commented on Feb 12
--
Not sure about you guys. But I just spend a couple hours narrowing it down.
Running LMODroid 4.2 on an LG V30+ (H930DS) with Kitsune Mask (Magisk Hide + Enforce SU List enabled), LSPosed, Play Integrity Fix, Hide My Applist (Snapchat hidden from everything and Applist Detector confirms) and Iconify.

I managed to get it to work with a failing Safety Net (not sure why - sometimes its working sometimes it isn't - have to fix this) and basic integrity on Play Integrity Checker. Could be because I spammed checked these services.

Regardless of what I did, the only thing that allowed me to login, was disabling USB Debugging in Developer Options. Quite strange but it worked. Even on the latest version. I might check with Hide My Applist to see if an option to hide this can be added in.

[mzashh](https://github.com/mzashh) commented on Feb 13
--
basically your device is banned, the ban is usually tied to IMEI but recently they have upped their security and they are using some other way to detect so spoofing won't help.

however i have a very easy fix for you and should work for root and non root as well, only works for device ban not permanent ban

1. basically download an old version of Snapchat from 2022 or something it should be v11.78.xxx something like that. [APKMirror link](https://www.apkmirror.com/apk/snap-inc/snapchat/snapchat-11-78-1-39-release/snapchat-11-78-1-39-3-android-apk-download/)
2. install it and you can login now since it doesn't have any device ban detection.
3. now after logging in you can update to the latest version via playstore or via apk and you should still be logged in.

note: if you uninstall Snapchat or logout you will have to follow these steps inorder to login again

[Verequies](https://github.com/Verequies) commented on Feb 13
--
@mzashh I'm not sure you understand. My device is certainly not banned. I am able to uninstall the app and install it from Play Store and log in successfully without any problems as long as I turn USB Debugging off or in terminal set `svc.init.adbd` to `stopped` or empty string by running `setprop init.svc.adbd stoppped`.

I have decompiled and am currently reverse engineering the app to check how they are checking for USB Debugging status properly. My hopes is that I can add a way to intercept this check into the [IAmNotADeveloper](https://github.com/xfqwdsj/IAmNotADeveloper/tree/main) Xposed module. That way all apps that do this kind of check will benefit from it.

[Calamardins](https://github.com/Calamardins) commented on Feb 13
--
Does anybody here know a solution to use Snapchat and SnapEnchance with emulators? I used Nox with kitsune (Nox emulator being the only one I could use Magisk on) but now I can't even login on Vainilla Snap with any emulators.

[mzashh](https://github.com/mzashh) commented on Feb 14
--
@Verequies thats weird, USB debugging or adb service seems to have no effect for me. i can login fine. the login issues is caused by a variety of different reasons, be it a device ban, play integrity issues, or in your case usb debugging. and the method i provided is a universal solution, definitely not a convenient solution but one that should definitely get you logged in for the time being.

[computer-catt](https://github.com/computer-catt) commented on Feb 17
--
I've had this issue a bit ago, the issue was that i had been trying to log into my account with the wrong password

[TheVisual](https://github.com/TheVisual) commented on Feb 18
--
**[Current Known Causes]:**
 
* Typing Wrong Password (Happend to me 0 root/jailbreak on both iOS / Android)
* Having Mutiple Accounts Signed in 2+ and logging in and out alot
* Not having `setprop init.svc.adbd stopped`
* Using Emulator (NEVER DO THIS)
* Not Passing Play Integrity (https://developer.android.com/google/play/integrity) (https://play.google.com/store/apps/details?id=gr.nikolasspyr.integritycheck&hl=en_US&gl=US) (Don't Ask me how to fix)
* Not Passing KeyAttestation (https://developer.android.com/privacy-and-security/security-key-attestation) (https://play.google.com/store/apps/details?id=io.github.vvb2060.keyattestation&hl=en_US&gl=US) (Don't Ask me how to fix) (Pixels always pass)
* Having Frida Enabled
* Having Unpinning Enabled
* custom rom/kernel go back to stock they hate custom roms (SOME Custom Rom's reportedly working NOT ALL lot's are broken) Please avoid custom roms if you can they will kill them eventually
* Using LSPatch Non Rooted Version
* Having Your Android_ID Banned For Breaking ToS (You need to change it or Factory Reset(BEST OPTION))
* Using VPN (Use a Residential / Mobile IP Logging in / Registering )
 
**Semi-Bypassing (Won't ALWAYS WORK) AS of 12.69 + there is a new Token so use this version to Bypass alot of detection then upgrade:**

* https://www.apkmirror.com/apk/snap-inc/snapchat/snapchat-12-63-0-55-release/
* Mirror 1 (Incase Snapchat Forces APKMirror to delete): https://workupload.com/file/5AKHvemZ7NU
* Mirror 2 (Incase Snapchat Forces APKMirror to delete): https://dmca.sh/WinO6/CISIgiDa47.apk
* Mirror 3 (Incase Snapchat Forces APKMirror to delete): https://s2.dosya.tc/server28/fdz99y/inAPI21_arm64-v8a_armeabi-v7a__nodpi__apkmirror.com.apk.html
* VirusTotal: https://www.virustotal.com/gui/file/524eef6b4f601d868b164cbe40b35af000df32a08f08bcc5a988e20ec45c127f

**[Recommended Rooting Option]:**

* https://github.com/HuskyDG/magisk-files (Magisk Delta It Has Magisk Hide Etc)

[Calamardins](https://github.com/Calamardins) commented on Feb 19
--
I was able to log in with an old phone while failing play integrity using dual messenger, with the main app I got the too many attempts msg but I was able to login in with the dual version, someone more skilled than me should look into this and the possibility of using apps like pararell space to bypass the login, I just did it by plain luck while playing around I got no technical knowledge.

[zenveh](https://github.com/zenveh) commented on Feb 23
--
Hi all, not too techy here but ive had this problem and just managed to fix it.
I am on a samsung s21 ultra normal stock ui and all, unrooted.
The account wasnt blocked as worked on separate unmodded phone, and normal play store snapchat worked
This problem occurred for me when i tried updating it.
I deleted both snapehance and snapchat(dont keey the files it asks if you want to otherwise itll be a pain to fix)
got newest stable builds apk of both
Install snapehance and toggle off all settings /anything changed
Turn off USB debugging (not my idea i read it here, dont know if turning off all snapenhance modules or this fixed it, maybe both)
I used lspatch and shizuku, shizuku works with wireless debugging
Use lspatch to install the mod, i didnt enable the option to downgrade as didnt want to risk it, dont know if it works
I believe thats all i did, i hope this helps others, ill try and help any more if can.
Gave me an error saying password was wrong which is odd because i know it's right, but just used phone number to login and all was well, toggled everything back on and works fine.

[authorisation](https://github.com/authorisation) commented on Feb 23
--
Hey guys, if anyone is experiencing the login issue, rhunk has made a userscript to fix this problem.
If you'd like to test it feel free to do so and let us know if it works.

Please keep in mind due to heavy obfuscation the script will take around 3 minutes (or more) to load up on launching Snapchat.

[loginfix_obf.zip](https://github.com/rhunk/SnapEnhance/files/14389208/loginfix_obf.zip)

[iamkcube](https://github.com/iamkcube) commented on Feb 25
--
Can someone say how to fix this?
Or how to run that login script. I have tried every version but it's still saying the same error again and again.

[iamkcube](https://github.com/iamkcube) commented on Feb 25
--
Okay, tried the solution from https://t.me/snapenhance_chat/1/50747 and it worked.

[Jeffdow85](https://github.com/Jeffdow85) commented on Mar 1
--
> Hey guys, if anyone is experiencing the login issue, rhunk has made a userscript to fix this problem.
> If you'd like to test it feel free to do so and let us know if it works.
> Please keep in mind due to heavy obfuscation the script will take around 3 minutes (or more) to load up on launching Snapchat.
>
> [loginfix_obf.zip](https://github.com/rhunk/SnapEnhance/files/14389208/loginfix_obf.zip)

Love that he has obfuscated it so snapchat can't see what he's done hahaha 🤣 Giving them a taste of their own medicine what a legend ❤️

[Jeffdow85](https://github.com/Jeffdow85) commented on Mar 1
--
> @authorisation how to/where to run script? the other solution did not work for me.

I opened it and looks like it goes in custom scripts folder where other Java script type singular mods people have been doing, go check the chat group on telegram about scripting for this bud I haven't tried them so haven't set up the working folder for it not even sure of the folder structure like where it goes although from memory reading a while ago think it just lives in enhance default save location so internal storage /snapenhance/scripts .. or something and must get loaded at login with Enhance patched snap.

[authorisation](https://github.com/authorisation) commented on Mar 2
--
> @authorisation how to/where to run script? the other solution did not work for me.

~Run it as a script using v2~ Script is patched, here's the unobfuscated version.

[loginfix.zip](https://github.com/rhunk/SnapEnhance/files/14470054/loginfix.zip)

[Techwizz-somboo](https://github.com/Techwizz-somboo) commented on Mar 7
--
> @Verequies thats weird, USB debugging or adb service seems to have no effect for me. i can login fine. the login issues is caused by a variety of different reasons, be it a device ban, play integrity issues, or in your case usb debugging. and the method i provided is a universal solution, definitely not a convenient solution but one that should definitely get you logged in for the time being.

This fixed it for me too

[Devilbager](https://github.com/Devilbager) commented on Mar 23
--
I was able to fix it by pressing the "optimize app" button with LSPatch with the newest snapchat version from the play store.

[FlashNUT](https://github.com/FlashNUT) commented on Apr 8
--
Hi guys. I am not as good as you are at these kind of things but what seemed to work for me was to create a new account after i installed snapchat and apply snapenhance to it. after i created that account i could easily change the account and it worked. Also i am on non rooted.

[bocajthomas](https://github.com/bocajthomas) commented on Apr 8
--
>  Hi guys. I am not as good as you are at these kind of things but what seemed to work for me was to create a new account after i installed snapchat and apply snapenhance to it. after i created that account i could easily change the account and it worked. Also i am on non rooted.

People Don't want to create a new account on each update 😒

[FlashNUT](https://github.com/FlashNUT) commented on Apr 8
--
> >  Hi guys. I am not as good as you are at these kind of things but what seemed to work for me was to create a new account after i installed snapchat and apply snapenhance to it. after i created that account i could easily change the account and it worked. Also i am on non rooted.
> 
> 
>  People Don't want to create a new account on each update 😒

well yes but. I created a new gmail adress and you can create as many snapchat accounts as you want with that email. after you create a snapchat account with that email you can log with your original snapchat account that you wanted to use without getting that error.

[Eatham532](https://github.com/Eatham532) commented on Apr 10
--
Closing the app and reopening worked for me. I have also tried changing the password not sure if that impacted

[krone0001](https://github.com/krone0001) commented on Apr 10
--
whats the latest fix for 2.0 cuz its doin it to me

[Eatham532](https://github.com/Eatham532) commented on Apr 10
--
> whats the latest fix for 2.0 cuz its doin it to me

Try to quit the app from the app drawer and reopen it.

[shaunbharat](https://github.com/shaunbharat) commented on Apr 10
--
I fixed this by going into developer settings and explicitly turning off wireless debugging. Then I logged in using my number instead of my username.

[NicoaraAlex](https://github.com/NicoaraAlex) commented on Apr 22
--
I found a workaround, it's simpler and it works for me: before logging out, I deactivate SnapEnhance from the LSPosed application, close and open the snap (in the bar) until the SnapEnhance settings wheel disappears, then log out or delete the memory on Snapchat. When I log in, I do the same, I activate SnapEnhance after logging into my Snapp account.

[amandy85](https://github.com/amandy85) commented on May 10
--
> > @authorisation how to/where to run script? the other solution did not work for me.
> 
> 
> ~Run it as a script using v2~
> Script is patched, here's the unobfuscated version.
> [loginfix.zip](https://github.com/rhunk/SnapEnhance/files/14470054/loginfix.zip)

🚨where to run that script ❓❓❓❓

[authorisation](https://github.com/authorisation) commented last month
--
@rhunk found some new ancient magic spell. Try this out if you're having issues on a custom rom.

[loginfix_v2_obfed.zip](https://github.com/rhunk/SnapEnhance/files/15341244/loginfix_v2_obfed.zip)
