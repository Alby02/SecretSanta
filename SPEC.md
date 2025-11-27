# Technical Specification: Secret Santa Zero-Knowledge

**Project Name:** Secret Santa Zero-Knowledge
**Version:** 1.0.0
**Language:** Kotlin
**Framework:** Android (Jetpack Compose)
**Backend:** Firebase (Auth, Firestore)
**Package Name:** `it.alby02.secretsanta`

---

## 1. Architectural Overview

The application follows a **Clean Architecture** pattern using MVVM (Model-View-ViewModel) and a single-Activity, Compose-driven UI.

### Layered Structure
*   **`ui` (Presentation Layer):** Contains Activities, Screens (Composables), ViewModels, and Navigation. Depends on `domain`.
*   **`domain` (Business Logic Layer):** Contains Repository interfaces and UseCases for business logic. Depends on nothing.
*   **`data` (Data Layer):** Contains Repository implementations, Data Sources (Firestore, Keystore), and Data Models. Depends on `domain`.

---

## 2. Directory Structure

```text
it.alby02.secretsanta
├── data
│   ├── model          // Data classes (UserProfile, Group)
│   ├── repository     // Repository Implementations (GroupRepositoryImpl)
│   └── security       // Cryptography Logic (CryptoManager)
├── domain
│   ├── repository     // Repository Interfaces (GroupRepository)
│   └── usecase        // Business logic (e.g., InitiateMatchingUseCase)
├── ui
│   ├── components     // Shared Composables
│   ├── features       // Screens and ViewModels
│   ├── navigation     // Navigation logic
│   └── theme          // Material 3 Theme
└── MainActivity.kt    // App Entry Point
```

---

## 3. Security & Cryptography Standards

This is a Zero-Knowledge application. The server must never have access to unencrypted private keys or the unencrypted master assignment list.

### Algorithms
*   **Symmetric Encryption:** `AES/GCM/NoPadding` (256-bit).
*   **Asymmetric Encryption:** `RSA` (stored in Android Keystore).
*   **Key Derivation:** `PBKDF2WithHmacSHA256`.
*   **Key Splitting:** `Shamir's Secret Sharing (SSS)`.

---

## 4. Firestore Database Schema (Sub-collection Model)

### `usersPublic` Collection
*   **Document ID:** `userId`
```json
{
  "username": "string",
  "publicKey": "string (Base64/PEM)"

}
```

### `usersLogin` Collection
*   **Document ID:** `userId`
```json
{
  "encryptedPrivateKey": "bytes (AES blob)",
  "pbkdfSalt": "bytes"
}
```

### `usersGroups` Collection
*   **Document ID:** `userId`
```json
{
  "groupIds": ["groupId1", "groupId2"]
}
```

### `groups` Collection
*   **Document ID:** `groupId`
```json
{
  "groupName": "string",
  "adminId": "string (userId)",
  "joinCode": "string",
  "members": ["userId1", "userId2"],
  "rules": [{ "giverId": "userIdA", "receiverId": "userIdB" }],
  "state": "string (enum: 'pending', 'assigned', 'recovery', 'completed')"
}
```

### Sub-collection: `groups/{groupId}/givers`
*   **Document ID:** `giverUserId`
```json
{
    "encryptedReceiverId": "bytes (AES blob, encrypted with the giver's unique AES key)",
    "encryptedKey": "bytes (The AES key, encrypted with the giver's public RSA key)",
    "encryptedShare": "bytes (SSS share of the MasterKey, encrypted with the member's public RSA key)"
}
```


### `groupsMasterLists` Collection
*   **Document ID:** `groupId`
```json
{
  "masterList": "string (AES blob Base64 encoded) | string (JSON array of assignments in the format: [{'giverId': 'userIdA', 'receiverId': 'userIdB'}])"
}
```

### Sub-collection: `groupsMasterLists/{groupId}/recoverySubmissions`
*   **Document ID:** `contributorId` (Temporary collection, cleared after use)
```json
{
    "encryptedShareForAdmin": "(The contributor's SSS share, re-encrypted for the admin)"
}
```

---

## 5. Detailed User Flows

### A. Registration (New User)
1.  **Trigger:** User provides email, password, and username.
2.  **Firebase Auth:** Create a new user with email and password.
3.  **Key Generation (On-Device):**
    *   Generate a new RSA public/private key pair.
    *   Generate a random salt for PBKDF2.
    *   Derive an AES key from the user's password and the salt using `PBKDF2WithHmacSHA256`.
    *   Encrypt the private key with this derived AES key.
4.  **Store Keys:**
    *   The **unencrypted private key** is immediately stored in the Android Keystore under a known alias. It is now available for the current session.
5.  **Firestore Transaction:**
    *   Create a document in `usersPublic` with the `userId`, `username`, and the public key.
    *   Create a document in `usersLogin` with the `userId`, the encrypted private key, and the PBKDF2 salt.
    *   Create an empty document in `usersGroups` for the new user.
6.  **UI:** Navigate to the main home screen.

### B. Login (Existing User)
1.  **Trigger:** User provides email and password.
2.  **Firebase Auth:** Sign in the user with email and password.
3.  **Key Decryption (On-Device):**
    *   Fetch the `encryptedPrivateKey` and `pbkdfSalt` from the `usersLogin` collection for the logged-in `userId`.
    *   Derive the AES key from the user's provided password and the fetched salt using `PBKDF2WithHmacSHA256`.
    *   Use the derived AES key to decrypt the `encryptedPrivateKey`.
4.  **Store Key in Keystore:**
    *   Store the now-decrypted private key in the Android Keystore. This key will persist in the Keystore until the user explicitly logs out.
5.  **UI:** Navigate to the main home screen.

### C. Logout
1.  **Trigger:** User clicks the "Logout" button.
2.  **Keystore:** Delete the user's private key from the Android Keystore.
3.  **Firebase Auth:** Sign the user out.
4.  **UI:** Navigate to the login screen.

### D. Application Lifecycle & Security
1.  **Logged-in State:** The user remains logged in even if the app is closed and reopened. The Firebase Auth state persists. The private key remains securely in the Android Keystore.
2.  **App Lock (Fingerprint):**
    *   The user has an option in the settings to enable fingerprint lock. This is disabled by default.
    *   If enabled, on app launch, the user must authenticate with their fingerprint to access the main content of the app.
    *   This lock provides a layer of UI security but does not affect the logged-in state or the storage of the private key in the Keystore. The user is still technically logged in behind the lock screen.
3.  **Session Key Access:** For any cryptographic operations requiring the user's private key (e.g., viewing an assignment, contributing a recovery share), the application will retrieve the key directly from the Android Keystore.

### E. Home Screen & Group List
1.  **Trigger:** Successful login or app start when already logged in.
2.  **Logic:**
    *   The `MainActivity` is displayed.
    *   A `HomeViewModel` observes the current user's `usersGroups/{userId}` document.
    *   As `groupIds` are fetched, it concurrently fetches the details for each group from the `groups` collection.
3.  **UI:**
    *   Displays a list of `Group` items. Each item shows the `groupName`.
    *   Displays two primary floating action buttons (or equivalent): "Create Group" and "Join Group".

### F. Create a Group (Admin)
1.  **Trigger:** User clicks the "Create Group" button.
2.  **UI:** An `AlertDialog` appears, prompting the user for a `groupName`.
3.  **UseCase:** `CreateGroupUseCase` is executed.
4.  **Logic:**
    *   Generate a unique, random, and user-friendly `joinCode`.
    *   Create a new document in the `groups` collection with:
        *   `groupName`: User-provided name.
        *   `adminId`: The current user's ID.
        *   `joinCode`: The newly generated code.
        *   `members`: A list containing only the admin's user ID.
        *   `state`: `"pending"`.
        *   `rules`: An empty list.
    *   Update the current user's `usersGroups/{userId}` document to add the new `groupId` to the `groupIds` array.
5.  **UI:** The dialog closes, and the home screen list automatically updates to show the new group.

### G. Join a Group (Member)
1.  **Trigger:** User clicks the "Join Group" button.
2.  **UI:** An `AlertDialog` appears, prompting the user for a `joinCode`.
3.  **UseCase:** `JoinGroupUseCase` is executed.
4.  **Logic:**
    *   Query the `groups` collection for a document where `joinCode` matches the user's input.
    *   If a group is found and the user is not already a member:
        *   Atomically add the current user's `userId` to the `members` array in the found group document.
        *   Atomically add the `groupId` to the `groupIds` array in the user's `usersGroups/{userId}` document.
    *   If no group is found, or the user is already a member, the use case returns a failure.
5.  **UI:** The dialog closes. On success, the home screen list automatically updates to show the newly joined group. On failure, a `Snackbar` or error message is displayed.

### H. Matching (Admin Only) - On-Device UseCase
1.  **Trigger:** Admin clicks "Start Matching".
2.  **UseCase:** `InitiateMatchingUseCase` is executed.
3.  **Logic (within Firestore Transactions):**
    *   Run matching algorithm locally on the admin's device.
    *   **Generate `MasterKey`:** Create a random AES key.
    *   Encrypt the full assignment list (the "master list") with the `MasterKey`.
    *   Create a new document in the `groupsMasterLists` collection with the `groupId` as the document ID, storing the encrypted master list in the `masterList` field.
    *   Split the `MasterKey` into N shares (where N is the number of members) using Shamir's Secret Sharing, with the required threshold set to `(N/2) + 1`.
    *   **For each member (who is a giver):**
        1.  Get the assignment: Giver -> Receiver.
        2.  Create a single-use AES key (`UserKey`).
        3.  Encrypt the Receiver's ID with the `UserKey` -> `encryptedReceiverId`.
        4.  Encrypt the `UserKey` with the Giver's public RSA key (fetched from `usersPublic`) -> `encryptedKey`.
        5.  Encrypt the Giver's SSS share of the `MasterKey` with their public RSA key -> `encryptedShare`.
        6.  Prepare a `create` operation for a new document `groups/{groupId}/givers/{giverId}` containing `encryptedReceiverId`, `encryptedKey`, and `encryptedShare`.
    *   Set the group `state` in the `groups/{groupId}` document to `"assigned"`.
    *   Commit all operations as a batch write.

### I. View Assignment (User)
1.  **Logic:**
    *   Fetch the single document `groups/{groupId}/givers/{currentUser.id}`.
    *   Decrypt the `encryptedKey` field using the user's private RSA key (from the device Keystore). This reveals the plaintext `UserKey`.
    *   Use the `UserKey` to decrypt the `encryptedReceiverId` field.
2.  **UI:** Display the name of the receiver.

### J. Admin Key Recovery (Secure, Admin-Initiated)
1.  **Initiation (Admin):** The admin starts the recovery process by setting the group `state` in the `groups/{groupId}` document to `"recovery"`.
2.  **Share Contribution (Members):**
    *   The UI notifies members that recovery has been initiated and prompts them to contribute.
    *   On each contributing member's device:
        1.  Fetch their document from `groups/{groupId}/givers/{memberId}`.
        2.  Decrypt the `encryptedShare` using their private key to get the plaintext SSS share.
        3.  Fetch the admin's public key from the `usersPublic/{adminId}` document.
        4.  Re-encrypt this plaintext share using the **admin's** public key.
        5.  Create a new document in the `groupsMasterLists/{groupId}/recoverySubmissions` sub-collection with their own user ID as the document ID, containing the re-encrypted share (`encryptedShareForAdmin`).
3.  **Threshold Check & Admin Confirmation:**
    *   The admin's device listens for changes in the `groupsMasterLists/{groupId}/recoverySubmissions` collection.
    *   The recovery threshold is met when the count of documents in `recoverySubmissions` plus one (for the admin themself) is greater than or equal to `(total number of members / 2) + 1`.
    *   Once the threshold is met, the UI must prompt the admin for a **final confirmation** before proceeding with decryption.
4.  **Reconstruction and Decryption (Admin):**
    *   **Only after admin confirmation**, the device gathers all the `encryptedShareForAdmin` documents from `recoverySubmissions` and decrypts each one with the admin's private key.
    *   The admin also fetches their *own* share from `groups/{groupId}/givers/{adminId}` and decrypts it with their private key.
    *   With the required number of plaintext shares (now meeting the SSS threshold), the admin's device reconstructs the `MasterKey`.
    *   The `MasterKey` is then used to decrypt the `masterList` from the `groupsMasterLists/{groupId}` document.
5.  **Finalization (Admin):**
    *   The decrypted list of assignments (in JSON format) overwrites the encrypted blob in the `masterList` field of the `groupsMasterLists/{groupId}` document.
    *   The temporary `groupsMasterLists/{groupId}/recoverySubmissions` sub-collection is deleted.
    *   The group `state` in the `groups/{groupId}` document is updated to `"completed"`.

---

## 6. Implementation Checklist

- [ ] `CryptoManager` Implementation (AES, RSA, PBKDF2, SSS).
- [ ] `GroupRepository` Implementation with Firestore (using sub-collections).
- [ ] **UseCase:** `InitiateMatchingUseCase` (on-device, transactional).
- [ ] **UseCase:** `AdminKeyRecoveryUseCase` (on-device, multi-party flow).
- [ ] UI implementation for all features.

---

## 7. UI/UX Guidelines
*   **Theme:** Material 3 (Purple/Pink baseline).
*   **Components:** Use `androidx.compose.material3`.
*   **Feedback:** Always use `CircularProgressIndicator` for async ops and `Snackbar` or inline text for errors.
*   **Dialogs:** Use `AlertDialog` for simple inputs (Create/Join).
