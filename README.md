# Save all the texts!
An Android app that exports all your SMS and MMS messages as an HTML page

**How to use this app**
This simple android app provides you with a simple way to backup all of your standard (SMS) and multimedia (MMS) messages as a HTML file.  The button contains a single button.  When pushed, it will create a directory called *SavedTexts* in the phone's local storage.  In this directory, the app will create an *index.html* file that contains all of your messages, grouped by conversation and sorted by date.  Each message indicates the number they came from and the content of the message.  Multimedia messages with images will created image files in that directory with corresponding image references in the HTML file.  When the export completes, you can plug your phone in as a USB drive as normal and copy the folder to your computer for viewing using any browser.

**Permissions/Security**
This app does not send any text messages, but it does require permission to read the ones you have.  The exported files are not transmitted anywhere, so it does not internet permission, but it does require permission to write to storage.  The exported file does contain all of your messages from all conversations.

**Compatibility**
The Android content providers for SMS and MMS are not well standardized and vary from device to device and between Android OS versions.  This was developed on a Samsung Galaxy S5 Active and may not work on other devices.


