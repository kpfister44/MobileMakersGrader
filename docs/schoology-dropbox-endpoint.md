# Schoology Dropbox Comment Endpoint

## Endpoint Information

**URL Pattern:**
```
https://schoology.d214.org/assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}
```

**Example:**
```
https://schoology.d214.org/assignment/8017693698/dropbox/view/130780673?destination=assignment%2F8017693698%2Finfo
```

**Method:** GET (when fetching) / POST (when submitting comments)

**Purpose:** Retrieve the dropbox viewer interface including submission details, existing comments, and forms for adding new comments/grades

---

## Response Structure

### Top-Level JSON Object

```json
{
  "title": string,
  "messages": string,
  "path": string,
  "path_query": string,
  "content": string (escaped HTML),
  "content_top": string,
  "content_top_upper": string,
  "content_right": string,
  "content_left": string,
  "content_left_top": string,
  "js": object,
  "css": object
}
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `title` | string | Page title (e.g., "Submissions") |
| `messages` | string | System messages (usually empty) |
| `path` | string | Internal path: `assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}` |
| `path_query` | string | Query parameters (usually empty) |
| `content` | string | **Main HTML content** - escaped HTML containing the entire dropbox viewer interface |
| `content_*` | string | Additional content regions (typically empty `<div></div>` wrappers) |
| `js` | object | JavaScript files, modules, and settings to load |
| `css` | object | CSS files to load |

---

## Content HTML Structure (Parsed)

The `content` field contains escaped HTML with the following major sections:

### 1. Dropbox Viewer Wrapper
```html
<div id="dropbox-viewer-wrapper">
```

### 2. Submission Activity Panel (Right Side)

```html
<div id="dropbox-viewer-right">
  <div id="dropbox-viewer-comments">
    <!-- Comments list -->
  </div>
  <!-- Comment form -->
</div>
```

#### Comment Structure

Each comment in the activity panel:

```html
<div class="comment" id="comment-{COMMENT_ID}">
  <div class="comment_picture">
    <!-- User profile picture -->
  </div>
  <div class="comment-contents">
    <div class="comment-comment">
      <div class="comment-top">
        <span class="comment-author">
          <a href="/user/{USER_ID}">{USER_NAME}</a>
        </span>
        <div class="comment-body-wrapper">
          {COMMENT_TEXT}
        </div>
      </div>
    </div>
  </div>
  <div class="comment-footer">
    <span class="comment-time">
      {TIMESTAMP}
    </span>
  </div>
</div>
```

**Example Comment Data:**
- Comment ID: `3370613977`
- User: Kyle Pfister (UID: `51173348`)
- Text: "Great work — all MVP requirements and every stretch goal are implemented..."
- Timestamp: "Today at 8:27 am"

#### Comment Form

The form for adding new comments includes:

```html
<form id="s-drop-item-add-comment-form"
      action="/assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}"
      method="post">

  <!-- Comment textarea -->
  <textarea name="comment" id="edit-comment-1"></textarea>

  <!-- File attachments -->
  <div id="attachments">
    <!-- File upload controls -->
  </div>

  <!-- CSRF Protection -->
  <input type="hidden" name="sid" value="{SESSION_ID}" />
  <input type="hidden" name="form_token" value="{CSRF_TOKEN}" />
  <input type="hidden" name="form_build_id" value="{BUILD_ID}" />

  <!-- Submit button -->
  <input type="submit" value="Post" />
</form>
```

**Critical Form Fields:**

| Field Name | Example Value | Purpose |
|------------|---------------|---------|
| `sid` | `35313137333334382a3138386537...` | Drupal session identifier (hex-encoded) |
| `form_token` | `06dcd0c2faaaab6dac9c9a2968113de8` | CSRF protection token |
| `form_build_id` | `e071a31-wZG4sVkNN80oDoKetO2T-...` | Drupal form build identifier |
| `comment` | (user input) | Comment text content |

### 3. Submission Viewer (Left Side)

```html
<div id="dropbox-viewer-left">
  <div id="dropbox-viewer-header">
    <!-- Section selector -->
    <select class="csm-toggle-sections">
      <option value="{COURSE_ID}">{COURSE_NAME}</option>
    </select>

    <!-- Student selector -->
    <select id="dropbox-viewer-user-select">
      <option value="{STUDENT_UID}">{STUDENT_NAME}</option>
    </select>

    <!-- Revision selector -->
    <select id="dropbox-viewer-revision-select">
      <option value="1">Revision 1</option>
    </select>

    <!-- File selector -->
    <select id="dropbox-viewer-item-select">
      <option value="{SUBMISSION_ID}">{FILENAME}</option>
    </select>
  </div>

  <!-- Submission metadata -->
  <div id="dropbox-viewer-submitted-date">
    Tuesday, September 30, 2025 at 10:35 am
  </div>

  <!-- File preview/download -->
  <div class="dropbox-viewer-item-wrapper">
    <!-- File content -->
  </div>
</div>
```

### 4. Grade Input Form

Located at bottom of left panel:

```html
<form id="s-grade-item-edit-enrollment-grade-form"
      action="/assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}"
      method="post">

  <!-- Grade input -->
  <input type="text"
         name="grade"
         id="edit-grade"
         value="10"
         original="10" />
  <span class="max-points">/10</span>

  <!-- Grade comment (private, not posted to activity feed) -->
  <textarea name="comment" id="edit-comment"></textarea>

  <!-- Show to student checkbox -->
  <input type="checkbox" name="comment_status" value="1" />

  <!-- CSRF tokens -->
  <input type="hidden" name="form_token" value="{CSRF_TOKEN}" />
  <input type="hidden" name="form_build_id" value="{BUILD_ID}" />

  <input type="submit" value="Submit" />
</form>
```

---

## JavaScript Settings Object

The `js.setting` object contains important configuration:

```javascript
{
  "s_common": {
    "csrf_key": "qEsWi1ySu0uOpOh2_0yN764_ZQBIlydIwdwV5mU307g",
    "csrf_token": "8d147d3dd1491b13904d85a9063e9418",
    "logout_token": "e1f1e9e84db1e151bd4d17b0ff770c70",
    "user": {
      "uid": "51173348",
      "school_nid": "26201007"
    }
  },
  "s_drop_item_viewer": {
    "grade_item_nid": "8017693698",
    "selected_uid": "130780673",
    "first_members": {
      "7921261672": 115532718,  // Course ID -> First student UID
      "7921330259": 138667042
    }
  },
  "s_attachment": {
    "max_filesize": 512,  // MB
    "max_num_uploads": 100,
    "upload_url": "/s_attachment_upload_chunked/2",
    "chunk_size": 70000
  }
}
```

---

## Key Identifiers in Example Response

| Identifier | Value | Description |
|------------|-------|-------------|
| Assignment ID | `8017693698` | The assignment being graded |
| Course IDs | `7921261672`, `7921330259` | Linked course sections |
| Student UID | `130780673` | Current student being viewed |
| Student Name | Roman-Avdii Tyryk | Student's full name |
| Student School UID | `s513390` | School-specific student ID |
| Instructor UID | `51173348` | Teacher user ID (Kyle Pfister) |
| Submission ID | `5280020834` | Specific file submission ID |
| Comment IDs | `3356325350`, `3365604905`, `3370613977` | Posted comment IDs |

---

## CSRF Token Extraction

**Three critical tokens required for POST requests:**

1. **Session ID (`sid`)** - Found in comment form:
   ```html
   <input type="hidden" name="sid"
          value="35313137333334382a3138386537336537653066313934613766353134646633336330333064363234" />
   ```

2. **Form Token (`form_token`)** - Found in each form:
   ```html
   <input type="hidden" name="form_token"
          value="06dcd0c2faaaab6dac9c9a2968113de8" />
   ```

3. **Form Build ID (`form_build_id`)** - Found in each form:
   ```html
   <input type="hidden" name="form_build_id"
          value="e071a31-wZG4sVkNN80oDoKetO2T-bn7Tl18YTxB5bGLoiaR-_Q" />
   ```

**Note:** Each form has its own unique `form_token` and `form_build_id`. These must be extracted from the specific form being submitted.

---

## Common Use Cases

### 1. Posting a Comment

**Endpoint:** `POST /assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}`

**Required Fields:**
- `comment` - Comment text
- `sid` - Session ID (hex-encoded)
- `form_token` - CSRF token from comment form
- `form_build_id` - Build ID from comment form
- `form_id` = `"s_drop_item_add_comment_form"`

### 2. Updating a Grade

**Endpoint:** `POST /assignment/{ASSIGNMENT_ID}/dropbox/view/{STUDENT_UID}`

**Required Fields:**
- `grade` - Numeric grade value
- `comment` - Grade comment (optional)
- `comment_status` - "1" to show comment to student
- `form_token` - CSRF token from grade form
- `form_build_id` - Build ID from grade form
- `form_id` = `"s_grade_item_edit_enrollment_grade_form"`

---

## Response Success Indicators

When a comment is successfully posted, the response JSON includes:

```json
{
  "status": true,
  "data": {
    // Updated HTML content
  }
}
```

**Console messages:**
- Success: `✓ Successfully posted comment for s519167 - Schoology confirmed status:true`
- Failure: `✗ Received HTML instead of JSON for s519167 - comment NOT saved`

---

## Notes

1. **Session Expiration:** Tokens typically expire after ~24 hours
2. **Form Tokens:** Must be extracted fresh from each form - cannot be reused
3. **Student UID Mapping:** Student UIDs must be mapped from enrollment data
4. **AJAX Flag:** Comment posting uses `drupal_ajax=1` parameter for JSON response
5. **HTML vs JSON:** Failed requests return HTML instead of JSON - check content type
