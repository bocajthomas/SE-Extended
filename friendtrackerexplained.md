## Friend Tracker Example ( When Someone enters the chat give a notification ) 
### Explanation 
This Example Allows the user to track there friends entering the chat through a notification 

### How To set up 
  1. Open Snapenhance
  2. Go To Features Tab
  3. Press on `Friend Tracker`
  4. Toggle on both `Record Messaging Events` & `Allow Running in Background`
  5. Toggle on `Friend Tracker` ( Make Sure You Toggle On Native Hooks Too ) 
  6. On the Home Tab, Press the 3 Dots next to `Quick Actions`
  7. Press the box next to `Friend Tracker`
  8. Tap the `Friend Tracker`
  9. Press `Rules`
  10. Tap `+ Add Rule`
  11. Press `Custom Rule` to name the rule 
  12. Set the `scope` to `All Friends/Groups` ( Note: this rule will be for everyone. There for you may get notification spammed ) 
  13. On the right side from `Events` Press `+`
  14. Set The `Type` To `conversation_enter` ( This Might be set by default )
  15. Press the Box next to `PUSH_NOTIFICATION`
  16. Under `Conditions`. Press the box next to `Only when im outside conversation`  ( This means you will get the notification only when your not on that conversation ) 
  17. Press `Add`
  18. Press `Save Rule`

### How To Use 
  1. When someone enters your chat you should get a notification which says `.......CONVERSATION_ENTER in DMs`
