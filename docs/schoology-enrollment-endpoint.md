# Schoology Course Enrollment Endpoint

## Endpoint Information

**URL Pattern:**
```
https://schoology.d214.org/iapi/enrollment/member_enrollments/course/{COURSE_ID}
```

**Example:**
```
https://schoology.d214.org/iapi/enrollment/member_enrollments/course/7921261672
```

**Method:** GET

**Purpose:** Retrieve all student enrollments for a specific course, including student UIDs, school UIDs, names, and enrollment metadata

**Authentication:** Requires valid session cookie and CSRF headers

---

## Response Structure

### Top-Level JSON Object

```json
{
  "response_code": 200,
  "body": {
    "{ENROLLMENT_ID}": {
      // Student enrollment object
    }
  }
}
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `response_code` | integer | HTTP response code (200 = success) |
| `body` | object | Object containing all enrollments, keyed by enrollment ID |

---

## Enrollment Object Structure

Each enrollment in the `body` object is keyed by the enrollment ID and contains:

```json
{
  "id": "3394039564",
  "uid": "130780673",
  "type": "2",
  "status": "1",
  "realm": "course",
  "realm_id": "7921261672",
  "school_uid": "s513390",
  "school_nid": "26201007",
  "created": "1708149519",
  "name_first": "Roman-Avdii",
  "name_last": "Tyryk",
  "name_title": "",
  "name_title_show": "0",
  "name_last_hide_full": "0",
  "name_middle": "",
  "name_middle_show": "0",
  "name_first_preferred": "",
  "use_preferred_first_name": "1",
  "picture_updated": "0",
  "name": "Roman-Avdii <b>Tyryk</b>",
  "picture_fid": null,
  "picture": "<!-- HTML for profile picture -->",
  "name_first_formatted": "Roman-Avdii"
}
```

### Enrollment Field Reference

| Field Name | Type | Description | Example Value |
|------------|------|-------------|---------------|
| `id` | string | **Enrollment ID** - Unique identifier for this enrollment | `"3394039564"` |
| `uid` | string | **User ID** - Schoology user ID for the student | `"130780673"` |
| `type` | string | **Role type** - `"2"` = Student, `"1"` = Teacher | `"2"` |
| `status` | string | **Enrollment status** - `"1"` = Active | `"1"` |
| `realm` | string | **Realm type** - Always `"course"` for course enrollments | `"course"` |
| `realm_id` | string | **Course ID** - The course this enrollment belongs to | `"7921261672"` |
| `school_uid` | string | **School-specific UID** - District student ID (e.g., `s513390`) | `"s513390"` |
| `school_nid` | string | **School node ID** - Schoology ID for the school | `"26201007"` |
| `created` | string | **Creation timestamp** - Unix timestamp when enrollment was created | `"1708149519"` |
| `name_first` | string | Student's first name | `"Roman-Avdii"` |
| `name_last` | string | Student's last name | `"Tyryk"` |
| `name_title` | string | Name title/prefix (rarely used) | `""` |
| `name_title_show` | string | Show title flag - `"0"` = hidden, `"1"` = shown | `"0"` |
| `name_last_hide_full` | string | Hide full last name flag | `"0"` |
| `name_middle` | string | Middle name | `""` |
| `name_middle_show` | string | Show middle name flag | `"0"` |
| `name_first_preferred` | string | Preferred first name (if different from legal name) | `""` |
| `use_preferred_first_name` | string | Use preferred name flag - `"1"` = use preferred | `"1"` |
| `picture_updated` | string | Profile picture last update timestamp (Unix) | `"0"` |
| `name` | string | **Formatted display name** - HTML formatted with bold last name | `"Roman-Avdii <b>Tyryk</b>"` |
| `picture_fid` | string/null | File ID for profile picture (`null` = default avatar) | `null` |
| `picture` | string | **HTML for profile picture** - Complete `<div>` with `<img>` tag | (see below) |
| `name_first_formatted` | string | Formatted first name (respects preferred name) | `"Roman-Avdii"` |

---

## Picture HTML Structure

The `picture` field contains escaped HTML:

```html
<div class="picture">
  <div class="profile-picture-wrapper">
    <div class="profile-picture">
      <a href="/user/130780673" title="Roman-Avdii Tyryk">
        <img src="https://asset-cdn.schoology.com/sites/all/themes/schoology_theme/images/user-default.svg"
             alt="Roman-Avdii Tyryk"
             title=""
             class="imagecache imagecache-profile_tiny" />
      </a>
    </div>
  </div>
</div>
```

**With Custom Profile Picture:**
```html
<img src="https://asset-cdn.schoology.com/system/files/imagecache/profile_tiny/pictures/picture-a718b6be313ea8e1fb3b5535e2125c69_68c32276c6c20.jpeg?1757618806"
     alt="Raya Lim" />
```

---

## Key Mappings for Grading

### UID to School UID Mapping

**Critical for grade posting:** You need to map Schoology UIDs to school UIDs.

```javascript
// Build mapping from UID to school_uid
const uidToSchoolUid = {};
Object.values(enrollments.body).forEach(enrollment => {
  uidToSchoolUid[enrollment.uid] = enrollment.school_uid;
});

// Example:
// uidToSchoolUid["130780673"] = "s513390"
```

### School UID Format

School UIDs follow the pattern: `s{NUMERIC_ID}`

Examples from response:
- `s513390` (Roman-Avdii Tyryk)
- `s519167` (Aryan Neupane)
- `s498776` (Claire Krawec)
- `s486002` (Marianna Castro)

**Note:** Some students may have empty `school_uid` (see example with Christina Y: `"school_uid": ""`). This typically indicates:
- Test accounts
- Guest accounts
- Incomplete enrollment records

---

## Enrollment Types and Status Codes

### Type Codes

| Type | Role | Description |
|------|------|-------------|
| `"1"` | Teacher/Instructor | Course administrator |
| `"2"` | Student | Regular student enrollment |
| `"3"` | TA/Assistant | Teaching assistant |
| `"4"` | Parent/Observer | Parent observer access |

### Status Codes

| Status | Meaning | Description |
|--------|---------|-------------|
| `"1"` | Active | Currently enrolled |
| `"0"` | Inactive | Dropped or removed |

---

## Example Students from Response

| Enrollment ID | UID | School UID | Name | Created (Unix) | Picture FID |
|---------------|-----|------------|------|----------------|-------------|
| `3394039564` | `130780673` | `s513390` | Roman-Avdii Tyryk | `1708149519` | `null` |
| `3394039558` | `115532718` | `s486002` | Marianna Castro | `1648617175` | `null` |
| `3394039561` | `124771084` | `s498776` | Claire Krawec | `1690916458` | `null` |
| `3394039562` | `124771090` | `s498783` | Nicholas Lesniak | `1690916459` | `null` |
| `3394114947` | `138667279` | `s520558` | Raya Lim | `1754313123` | `6292457175` |
| `3475024959` | `112851584` | (empty) | Christina Y | `1631514037` | `null` |

---

## Common Use Cases

### 1. Fetching All Course Enrollments

**Request:**
```http
GET /iapi/enrollment/member_enrollments/course/7921261672
Cookie: {session_cookie}
x-csrf-key: {csrf_key}
x-csrf-token: {csrf_token}
```

**Purpose:** Get all students enrolled in the course for:
- Building UID-to-school-UID mapping
- Displaying class roster
- Bulk grade operations

### 2. Building UID Mapping for Grade Posting

**Java Example:**
```java
Map<String, String> uidToSchoolUid = new HashMap<>();

for (Enrollment enrollment : enrollments) {
    if ("2".equals(enrollment.getType()) && "1".equals(enrollment.getStatus())) {
        // Only active students
        String uid = enrollment.getUid();
        String schoolUid = enrollment.getSchoolUid();

        if (schoolUid != null && !schoolUid.isEmpty()) {
            uidToSchoolUid.put(uid, schoolUid);
        }
    }
}
```

### 3. Getting Student Display Names

**Handling Preferred Names:**

The `use_preferred_first_name` flag determines which name to display:
- If `"1"` and `name_first_preferred` is not empty: use preferred name
- Otherwise: use `name_first`

The API provides `name_first_formatted` which already handles this logic.

**Display Format:**
```
{name_first_formatted} {name_last}
```

Examples:
- `"Roman-Avdii Tyryk"`
- `"Marianna Castro"`
- `"Claire Krawec"`

---

## Filtering Enrollments

### Get Only Active Students

```java
List<Enrollment> activeStudents = enrollments.values().stream()
    .filter(e -> "2".equals(e.getType()))      // Type 2 = Student
    .filter(e -> "1".equals(e.getStatus()))    // Status 1 = Active
    .filter(e -> !e.getSchoolUid().isEmpty()) // Has school UID
    .collect(Collectors.toList());
```

### Get Students with Custom Profile Pictures

```java
List<Enrollment> withPictures = enrollments.values().stream()
    .filter(e -> e.getPictureFid() != null)
    .collect(Collectors.toList());
```

### Sort Students Alphabetically

```java
List<Enrollment> sorted = enrollments.values().stream()
    .filter(e -> "2".equals(e.getType()))
    .sorted(Comparator.comparing(Enrollment::getNameLast)
                      .thenComparing(Enrollment::getNameFirstFormatted))
    .collect(Collectors.toList());
```

---

## Response Statistics (Example Data)

**Total Enrollments:** 33

**Breakdown:**
- Active students: 32
- Students with school UID: 32
- Students with custom picture: 1 (Raya Lim)
- Students with empty school UID: 1 (Christina Y)
- Students with middle names: 19

**Enrollment Dates:**
- Oldest: `1631514037` (Sep 13, 2021) - Christina Y
- Newest: `1754313266` (Oct 4, 2025) - Jaxson Wetendorf

---

## Error Responses

### 401 Unauthorized

```json
{
  "response_code": 401,
  "body": {}
}
```

**Cause:** Invalid session cookie or expired CSRF tokens

**Solution:** Re-authenticate and extract fresh tokens

### 403 Forbidden

```json
{
  "response_code": 403,
  "body": {}
}
```

**Cause:** User does not have permission to view course enrollments

**Solution:** Ensure the authenticated user is an instructor for the course

### 404 Not Found

```json
{
  "response_code": 404,
  "body": {}
}
```

**Cause:** Course ID does not exist

**Solution:** Verify the course ID is correct

---

## Integration with Grade Posting

### Workflow for Auto-Grading System

1. **Fetch enrollments:**
   ```
   GET /iapi/enrollment/member_enrollments/course/{COURSE_ID}
   ```

2. **Build UID mappings:**
   ```java
   Map<String, String> uidToSchoolUid = new HashMap<>();
   Map<String, String> schoolUidToName = new HashMap<>();

   for (Enrollment e : enrollments.body.values()) {
       uidToSchoolUid.put(e.uid, e.school_uid);
       schoolUidToName.put(e.school_uid,
                          e.name_first_formatted + " " + e.name_last);
   }
   ```

3. **Grade each student:**
   - Read student submissions from local folder
   - Extract school UID from folder name (e.g., `"Student Name - s513390"`)
   - Grade the assignment
   - Look up Schoology UID: `uidToSchoolUid.get(schoolUid)`

4. **Post grades:**
   ```
   PUT /iapi/grades/grader_grade_data/{COURSE_ID}/{GRADING_PERIOD_ID}
   ```
   Use the student UID from mapping

---

## Caching Considerations

**Recommended caching strategy:**
- Cache enrollment data for 1-24 hours
- Refresh when:
  - New students enroll
  - Students drop the course
  - Grade posting fails with "student not found"

**Cache key format:**
```
enrollment_cache:course:{COURSE_ID}:{TIMESTAMP}
```

---

## Name Display Rules

### Official Name Construction

```
[title] [first/preferred] [middle?] [last]
```

**Flags controlling display:**
- `name_title_show`: Show title prefix
- `use_preferred_first_name`: Use preferred instead of legal first name
- `name_middle_show`: Show middle name
- `name_last_hide_full`: Hide full last name (privacy)

### Examples

**Standard:**
- Input: `name_first="Roman-Avdii"`, `name_last="Tyryk"`
- Output: `"Roman-Avdii Tyryk"`

**With Preferred Name:**
- Input: `name_first="Robert"`, `name_first_preferred="Bob"`, `use_preferred_first_name="1"`
- Output: `"Bob LastName"`

**With Middle Name Shown:**
- Input: `name_first="Nicholas"`, `name_middle="Joseph"`, `name_middle_show="1"`
- Output: `"Nicholas Joseph Lesniak"`

---

## Related Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/iapi/enrollment/member_enrollments/course/{COURSE_ID}` | Get all enrollments (this endpoint) |
| `/iapi/enrollment/users/{UID}` | Get single user's enrollment details |
| `/iapi/grades/grader_grade_data/{COURSE_ID}/{PERIOD_ID}` | Post grades for students |
| `/assignment/{ASSIGNMENT_ID}/dropbox/view/{UID}` | View student submission |

---

## Notes

1. **School UID is critical:** Required for posting grades and comments to Schoology
2. **Empty school UIDs:** Some accounts (test/guest) may lack school UIDs - skip these when grading
3. **UID vs School UID:** Always use **UID** for API calls, but match students using **school UID**
4. **Enrollment IDs:** These are separate from UIDs - enrollment ID is the relationship record
5. **Type filtering:** Always filter for `type="2"` to get only students
6. **Status filtering:** Filter for `status="1"` to exclude dropped students
7. **Profile pictures:** Most students use default avatar (`picture_fid=null`)
8. **Name formatting:** Use `name_first_formatted` - it handles preferred names automatically
9. **Response size:** Large courses (100+ students) may return 50KB+ of JSON data
10. **Session expiration:** This endpoint requires valid session - tokens expire after ~24 hours

---

## Sample Response Summary

**Course:** 7921261672 (09040 2 COMP PRG MOB APP)

**Student Count:** 32 active students

**Sample School UIDs:**
- s519779, s519497, s525722, s519613, s523849
- s485978, s520992, s486002, s519538, s523693
- s519600, s486286, s519555, s498776, s498783
- s520558, s519451, s523728, s519167, s486652
- s518721, s509599, s495748, s520513, s497807
- s518996, s486956, s513390, s523778, s520799
- s521022

**Students with Custom Pictures:**
- Raya Lim (picture_fid: 6292457175)

**Students without School UID:**
- Christina Y (likely test/guest account)
