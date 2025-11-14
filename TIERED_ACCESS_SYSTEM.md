# DuckBot Tiered Access System

## Overview

DuckBot now supports optional login with a **tiered user access system**. Users can start the application as guests with read-only access, or log in to unlock full features based on their tier level.

## Key Features

✅ **Optional Login** - Start without credentials as a guest  
✅ **No Login Required** - Guest access provides useful read-only features  
✅ **Feature-Based Access Control** - Each tier unlocks specific features  
✅ **Hierarchical Tiers** - Higher tiers have all features of lower tiers  
✅ **Easy to Extend** - Add new features via `UserTier.Feature` enum  

---

## User Tiers

### 1. GUEST (No Login)
**Requirements:** None - automatic when selecting "Continue as Guest"

**Features:**
- ✅ View bot list (read-only)
- ✅ View logs
- ✅ View updates
- ❌ Create/edit/delete bots
- ❌ Run scripts
- ❌ Build scripts
- ❌ Access advanced features

**Use Case:** Demo mode, read-only monitoring, reviewing logs

### 2. FREE (Default Login)
**Requirements:** Valid login credentials

**Features:**
- ✅ View bots
- ✅ Create bots
- ✅ View logs
- ✅ Run scripts (LIMITED to 2 concurrent runs)
- ✅ View updates
- ❌ Edit/delete bots
- ❌ Build scripts
- ❌ Advanced popup solver
- ❌ Export scripts

**Use Case:** Basic automation, testing, personal use

### 3. PREMIUM
**Requirements:** Valid login + premium tier assignment

**Features:**
- ✅ Full bot management (create, edit, delete)
- ✅ Unlimited script execution
- ✅ Script builder with all step types
- ✅ Advanced popup solver
- ✅ Export/import scripts
- ✅ OCR features
- ✅ Logs and monitoring
- ✅ Auto-updates

**Use Case:** Power users, team automation, advanced scripting

### 4. ADMIN
**Requirements:** Valid login + admin role

**Features:**
- ✅ All features unlocked
- ✅ User management
- ✅ System settings
- ✅ All premium features
- ✅ Future expansion features

**Use Case:** System administrators, developers

---

## Feature Hierarchy

```
┌─────────────────────────────────────────────────────────┐
│                       ADMIN                             │
│              All features unlocked                       │
└─────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────┐
│                       PREMIUM                           │
│  Edit bots, unlimited runs, script builder, advanced   │
└─────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────┐
│                       FREE                              │
│   Create bots, limited script runs, basic features     │
└─────────────────────────────────────────────────────────┘
                            ▲
                            │
┌─────────────────────────────────────────────────────────┐
│                       GUEST                             │
│          View-only access (read-only)                   │
└─────────────────────────────────────────────────────────┘
```

---

## Login Screen

The updated login screen shows:

1. **Login Form** - Username/password fields for authenticated access
2. **Continue as Guest** - Button for read-only access
3. **Guest Features Info** - Quick reference of what guests can do

```
┌─────────────────────────────────────────┐
│     DuckBot Control Center              │
│  Log in or continue as guest            │
├─────────────────────────────────────────┤
│                                         │
│  Username: [________________]           │
│  Password: [________________]           │
│                                         │
│  [Login]        [Continue as Guest]    │
│                                         │
│  Guest Account Features:                │
│   • View bot list (read-only)           │
│   • View logs                           │
│   • View updates                        │
│   • No script execution or editing      │
│                                         │
└─────────────────────────────────────────┘
```

---

## UI Updates

### Header with User Info
The main window shows a header with:
- Current username
- Current tier level  
- Logout button

Example: `User: Duck (Free User)`

### Feature Restrictions
When a feature is restricted:

1. **Buttons become disabled** - Add, Delete, Run buttons gray out for guests
2. **Tabs show overlay** - Restricted tabs show a lock icon with explanation
3. **Clear messaging** - "Feature Restricted" dialog explains why

Example disabled buttons:
```
[Add] [Delete] [Run]
 ✗      ✗      ✗     (disabled for guests)
```

Example restricted tab overlay:
```
┌─────────────────────────────┐
│           🔒                │
│                             │
│   Feature Restricted        │
│                             │
│   Your current tier         │
│   (Guest) does not have     │
│   access to Script Builder  │
│                             │
│   Please log in with a      │
│   higher tier account       │
└─────────────────────────────┘
```

---

## Implementation Details

### Core Classes

#### `UserTier.java` (enum)
Defines tier levels and their features:
```java
public enum UserTier {
    GUEST(0, "Guest (Read-Only)", ...),
    FREE(1, "Free User", ...),
    PREMIUM(2, "Premium User", ...),
    ADMIN(3, "Administrator", ...)
}
```

#### `UserTier.Feature` (enum)
Individual features that can be restricted:
- `VIEW_BOTS`, `CREATE_BOT`, `EDIT_BOT`, `DELETE_BOT`, `RUN_BOT`
- `VIEW_SCRIPTS`, `CREATE_SCRIPT`, `EDIT_SCRIPT`, `DELETE_SCRIPT`
- `RUN_SCRIPT_LIMITED`, `RUN_SCRIPT_UNLIMITED`
- `ADVANCED_POPUP_SOLVER`, `EXPORT_SCRIPTS`, `OCR_FEATURES`
- `VIEW_LOGS`, `EXPORT_LOGS`, `VIEW_SETTINGS`, `EDIT_SETTINGS`
- And more...

#### `FeatureAccess.java` (class)
Manages feature permissions:
```java
public class FeatureAccess {
    public Set<UserTier.Feature> allowedFeatures;
    
    public boolean hasAccess(UserTier.Feature feature) { ... }
    public boolean hasAccessAll(UserTier.Feature... features) { ... }
    public boolean hasAccessAny(UserTier.Feature... features) { ... }
}
```

#### `User.java` (model)
Updated with tier field:
```java
public final class User {
    public String username;
    public String role;
    public UserTier tier;  // ← NEW
    public Instant createdUtc;
}
```

#### `AuthService.java` (service)
Enhanced with tier methods:
```java
public boolean login(String username, String password) throws AuthException { ... }
public void loginAsGuest() { ... }  // ← NEW
public boolean isLoggedIn() { ... }  // ← NEW
public boolean hasAccess(UserTier.Feature feature) { ... }  // ← NEW
public UserTier getCurrentTier() { ... }  // ← NEW
public void setUserTier(UserTier tier) throws AuthException { ... }  // ← NEW
```

#### `DuckBotApp.java` (UI)
Updated with:
- `showLogin()` - New login screen with guest option
- `createHeader()` - Shows user tier info
- `checkFeatureAccess()` - Helper to validate feature access
- `createRestrictedOverlay()` - Display when features are blocked
- Tab creation methods now check feature access

---

## Feature Access Examples

### Check if user can run scripts
```java
if (bootstrap.getAuthService().hasAccess(UserTier.Feature.RUN_SCRIPT_UNLIMITED)) {
    // Run unlimited scripts
} else if (bootstrap.getAuthService().hasAccess(UserTier.Feature.RUN_SCRIPT_LIMITED)) {
    // Run up to 2 scripts
} else {
    // Cannot run scripts
}
```

### Disable button for restricted features
```java
Button addBotBtn = new Button("Add Bot");
boolean canCreate = bootstrap.getAuthService().hasAccess(UserTier.Feature.CREATE_BOT);
addBotBtn.setDisable(!canCreate);
```

### Check current tier
```java
UserTier tier = bootstrap.getAuthService().getCurrentTier();
if (tier.isAtLeast(UserTier.PREMIUM)) {
    // Premium and above features
}
```

### Get tier display name
```java
String tierName = bootstrap.getAuthService().getCurrentTier().displayName;
System.out.println("Your tier: " + tierName);  // "Guest (Read-Only)" or "Free User" etc.
```

---

## Adding New Features

### Step 1: Add to UserTier.Feature enum
```java
public enum Feature {
    // ... existing features
    MY_NEW_FEATURE  // ← Add here
}
```

### Step 2: Include in tier definitions
```java
PREMIUM(2, "Premium User", new FeatureAccess() {
    {
        allowedFeatures = EnumSet.of(
            // ... other features
            Feature.MY_NEW_FEATURE  // ← Include here
        );
    }
})
```

### Step 3: Check access in UI
```java
if (!bootstrap.getAuthService().hasAccess(UserTier.Feature.MY_NEW_FEATURE)) {
    return createRestrictedOverlay("My New Feature");
}
```

---

## Tier Assignment

Currently, all logins default to **FREE** tier. To assign different tiers:

### Option 1: Based on username
Modify `AuthService.login()`:
```java
public boolean login(String username, String password) throws AuthException {
    if (provider.login(username, password)) {
        User user = new User(username, "USER", 
            username.equals("admin") ? UserTier.ADMIN : UserTier.FREE,  // ← Conditional
            java.time.Instant.now()
        );
        currentUser.set(user);
        return true;
    }
    return false;
}
```

### Option 2: Store in user database
Extend `User` model and persist tier to `users.json`:
```json
{
  "users": [
    {"username": "duck", "password_hash": "...", "tier": "PREMIUM"},
    {"username": "admin", "password_hash": "...", "tier": "ADMIN"}
  ]
}
```

### Option 3: Runtime assignment (admin only)
```java
// Admin sets user tier
bootstrap.getAuthService().setUserTier(UserTier.PREMIUM);
```

---

## Default Behavior

1. **On First Launch:**
   - User sees login screen
   - Can choose to log in or continue as guest
   
2. **Guest Path:**
   - Gets GUEST tier automatically
   - Can view bots, logs, updates
   - Cannot modify anything
   
3. **Login Path:**
   - Enters username/password
   - Gets FREE tier by default
   - Can create bots, limited script execution
   
4. **After Logout:**
   - Returns to login screen
   - Can log in again or be guest again

---

## Future Enhancements

1. **Tier Persistence** - Save tier to user.json
2. **Subscription Management** - UI to upgrade tiers
3. **Custom Permissions** - Override tier defaults per user
4. **Tier Analytics** - Track feature usage by tier
5. **Time-based Tiers** - Trial periods, expiration dates
6. **Team Features** - Shared tier access for team members

---

## Testing

### Test Guest Login
1. Launch DuckBot
2. Click "Continue as Guest"
3. Verify: Can view bots, logs, updates
4. Verify: Add/Delete/Run buttons are disabled
5. Verify: Script Builder shows lock icon

### Test Free Login
1. Launch DuckBot  
2. Log in with: `Duck` / `Aedyn2013`
3. Verify: Can create and edit bots
4. Verify: Can run scripts (limited display)
5. Verify: Script Builder is available

### Test Feature Restrictions
1. As guest, check that:
   - Bot buttons are disabled
   - Script Builder shows overlay
   - Settings/Advanced features blocked
2. As free user, check that:
   - Can create bots
   - Can run scripts
   - Cannot access premium features

---

## Summary

The tiered access system provides:

✅ **Flexibility** - Users can try as guest, upgrade by logging in  
✅ **Security** - Features protected behind authentication  
✅ **Extensibility** - Easy to add new features and tiers  
✅ **User Experience** - Clear messaging about restrictions  
✅ **Scalability** - Ready for subscription/monetization  

Users can now try DuckBot immediately as a guest, with full functionality available after login!
