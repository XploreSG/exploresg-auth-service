# Driver Details Collection - Deferred Onboarding

## Overview

Driver details (phone, date of birth, driving license number) are now **optional at first sign-in** to reduce friction. Users can provide them later when needed (e.g., before booking a vehicle).

## User Flow

### 1. First Sign-In (No Friction!)

```
User signs in via Google SSO
    ↓
Backend creates User with Role.USER
    ↓
Returns JWT
    ↓
Front-end: User enters app immediately ✅
(No profile form blocking them)
```

### 2. Later: Booking a Vehicle

```
User tries to book a vehicle
    ↓
Front-end checks: Does user have drivingLicenseNumber?
    ├─ Yes → Proceed with booking
    └─ No  → Show driver details form
             ↓
             User fills: phone, DOB, license
             ↓
             POST /api/v1/signup with JWT
             ↓
             Proceed with booking
```

## API Contract

### Endpoint: `POST /api/v1/signup`

**Auth:** `Authorization: Bearer <jwt>` (required)

**All fields are now optional:**

```json
{
  "phone": "91234567",
  "dateOfBirth": "1990-01-01",
  "drivingLicenseNumber": "S1234567A",
  "passportNumber": "E1234567",
  "preferredLanguage": "en",
  "countryOfResidence": "SG"
}
```

**Response:**

```json
{
  "userId": 42,
  "email": "user@example.com",
  "givenName": "Alice",
  "familyName": "Tan",
  "picture": "https://...",
  "phone": "91234567",
  "dateOfBirth": "1990-01-01",
  "drivingLicenseNumber": "S1234567A",
  "passportNumber": "E1234567",
  "preferredLanguage": "en",
  "countryOfResidence": "SG"
}
```

## Front-End Implementation

### On Sign-In

```javascript
// After Google SSO success
const { jwt, user } = await auth.signIn();

// Store JWT
localStorage.setItem("jwt", jwt);

// Redirect to app (no profile form)
router.push("/dashboard");
```

### Before Booking

```javascript
async function initiateBooking(vehicleId) {
  const user = await api.get("/api/v1/me", {
    headers: { Authorization: `Bearer ${jwt}` },
  });

  // Check if driver details exist
  if (!user.drivingLicenseNumber) {
    // Show driver details form
    showDriverDetailsModal();
  } else {
    // Proceed with booking
    proceedToBooking(vehicleId);
  }
}

async function submitDriverDetails(formData) {
  await api.post(
    "/api/v1/signup",
    {
      phone: formData.phone,
      dateOfBirth: formData.dateOfBirth,
      drivingLicenseNumber: formData.drivingLicenseNumber,
    },
    {
      headers: { Authorization: `Bearer ${jwt}` },
    }
  );

  // Now proceed with booking
  proceedToBooking(vehicleId);
}
```

## Backend Changes

**File:** `src/main/java/com/exploresg/authservice/dto/SignupProfileRequest.java`

- ✅ Removed `@NotBlank` from `phone`
- ✅ Removed `@NotNull` from `dateOfBirth`
- ✅ Removed `@NotBlank` from `drivingLicenseNumber`

**File:** `src/main/java/com/exploresg/authservice/service/AuthenticationService.java`

- ✅ Added null-check for `dateOfBirth` before calling `.toString()`
- ✅ Prevents `NullPointerException` when profile exists but dateOfBirth is null

**Result:**

- All profile fields are now optional
- Users can sign in and use the app immediately
- Driver details can be collected later when needed
- No runtime errors when fields are null

## Validation Strategy

### Backend

- No validation at signup endpoint
- Profile fields stored as NULL if not provided
- Can be updated later via same endpoint

### Front-End (Recommended)

- Validate driver details **before booking** (business logic layer)
- Ensure required fields for booking:
  - Phone number (contact)
  - Date of birth (age verification)
  - Driving license number (legal requirement)

### Booking Service

- When user initiates booking, check:
  ```java
  if (user.getProfile().getDrivingLicenseNumber() == null) {
      throw new ProfileIncompleteException("Driver details required for booking");
  }
  ```

## Benefits

✅ **Reduced friction** - Users can explore app immediately after sign-in  
✅ **Better UX** - Only ask for driver details when actually needed  
✅ **Same backend** - No breaking changes, just removed validation  
✅ **Flexible** - Can still collect all details upfront if needed

## Testing

### Test 1: Sign-in without profile

```bash
# After Google SSO (with JWT)
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{}'

# Expected: 200 OK (profile created with NULL fields)
```

### Test 2: Update profile later

```bash
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "91234567",
    "dateOfBirth": "1990-01-01",
    "drivingLicenseNumber": "S1234567A"
  }'

# Expected: 200 OK (profile updated with driver details)
```

## Migration Notes

- **Existing users:** No impact (they already have driver details)
- **New users:** Can skip driver details at signup
- **Database:** No schema changes needed (fields allow NULL)
- **Front-end:** Update signup flow to skip driver form initially

---

**Last Updated:** October 18, 2025
