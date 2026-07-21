# Tour Package Module — Frontend API Reference

> Base URL: `/api`
>
> All responses follow the standard envelope:
> ```json
> { "status": "success", "message": "...", "data": { ... } }
> ```

---

## Table of Contents

1. [Tour Flags](#1-tour-flags)
2. [Tour View Counter](#2-tour-view-counter)
3. [User Favorites](#3-user-favorites)
4. [Tour Categories](#4-tour-categories)
5. [Tour Package — Updated Fields](#5-tour-package--updated-fields)

---

## 1. Tour Flags

Flags highlight tours across the website. Supported values: **`POPULAR`**, **`FEATURED`**.

### Get Tours by Flag (Public)

```
GET /api/public/tour-packages/by-flag?flag={FLAG}
```

**Query Parameters**

| Param | Type   | Required | Description                      |
|-------|--------|----------|----------------------------------|
| flag  | string | Yes      | `POPULAR` or `FEATURED`          |

**Example Request**

```
GET /api/public/tour-packages/by-flag?flag=POPULAR
```

**Example Response**

```json
{
  "status": "success",
  "message": "Tour packages by flag retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Cox's Bazar Beach Escape",
      "flags": ["POPULAR", "FEATURED"],
      "viewCount": 142,
      "categories": [
        { "id": 2, "name": "Beach Holiday" }
      ],
      "..."
    }
  ]
}
```

> Only **PUBLISHED** packages are returned by this endpoint.

---

### Assigning Flags (Admin — Create / Update Tour Package)

Pass `flags` in the tour package create/update request body.

```
POST /api/tour-packages
PUT  /api/tour-packages/{id}
```

**Request Body (relevant fields)**

```json
{
  "title": "Cox's Bazar Beach Escape",
  "flags": ["POPULAR", "FEATURED"],
  "categoryIds": [1, 3],
  "..."
}
```

| Field       | Type            | Description                              |
|-------------|-----------------|------------------------------------------|
| flags       | `string[]`      | Array of `POPULAR` and/or `FEATURED`     |
| categoryIds | `number[]`      | IDs of categories to assign to the tour  |

---

## 2. Tour View Counter

The view count is **automatically incremented** every time the public tour detail page is fetched. No separate call is needed for tracking.

### Get View Count for a Tour (Public)

```
GET /api/public/tour-package/{id}/views
```

**Path Parameters**

| Param | Type   | Description      |
|-------|--------|------------------|
| id    | number | Tour package ID  |

**Example Response**

```json
{
  "status": "success",
  "message": "Tour view count retrieved successfully",
  "data": 142
}
```

> The view count is also included directly in all tour package responses as `viewCount`.

---

## 3. User Favorites

All favorites endpoints require **authentication** (JWT Bearer token).

### Add to Favorites

```
POST /api/user/tour-favorites/{tourPackageId}
```

**Headers**

```
Authorization: Bearer <token>
```

**Example Response**

```json
{
  "status": "success",
  "message": "Tour package added to favorites",
  "data": null
}
```

---

### Remove from Favorites

```
DELETE /api/user/tour-favorites/{tourPackageId}
```

**Headers**

```
Authorization: Bearer <token>
```

**Example Response**

```json
{
  "status": "success",
  "message": "Tour package removed from favorites",
  "data": null
}
```

---

### Get All Favorites

```
GET /api/user/tour-favorites
```

**Headers**

```
Authorization: Bearer <token>
```

**Example Response**

```json
{
  "status": "success",
  "message": "User favorites retrieved successfully",
  "data": [
    {
      "id": 10,
      "tourPackageId": 3,
      "tourTitle": "Sundarban Wildlife Adventure",
      "destinationCity": "Khulna",
      "destinationCountry": "Bangladesh",
      "thumbnailUrl": "https://cdn.example.com/images/sundarban.jpg",
      "favoritedAt": "2026-07-05T10:30:00"
    }
  ]
}
```

**Favorite Object**

| Field          | Type     | Description                          |
|----------------|----------|--------------------------------------|
| id             | number   | Favorite record ID                   |
| tourPackageId  | number   | ID of the tour package               |
| tourTitle      | string   | Title of the tour                    |
| destinationCity | string  | Destination city                     |
| destinationCountry | string | Destination country               |
| thumbnailUrl   | string   | First media image URL (may be null)  |
| favoritedAt    | datetime | When the user favorited it           |

---

### Check Favorite Status

Useful for rendering a filled/empty heart icon on tour cards.

```
GET /api/user/tour-favorites/{tourPackageId}/status
```

**Headers**

```
Authorization: Bearer <token>
```

**Example Response**

```json
{
  "status": "success",
  "message": "Favorite status retrieved successfully",
  "data": true
}
```

---

## 4. Tour Categories

### Public Endpoints

#### List All Active Categories

```
GET /api/public/tour-categories
```

**Query Parameters**

| Param     | Type    | Default | Description                              |
|-----------|---------|---------|------------------------------------------|
| withTours | boolean | false   | If `true`, each category includes its tour list |

**Example Response**

```json
{
  "status": "success",
  "message": "Tour categories retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Honeymoon",
      "description": "Romantic getaways for couples",
      "isActive": true,
      "createdAt": "2026-07-01T12:00:00",
      "updatedAt": "2026-07-01T12:00:00",
      "tours": null
    },
    {
      "id": 2,
      "name": "Adventure",
      "description": "Thrilling outdoor experiences",
      "isActive": true,
      "tours": null
    }
  ]
}
```

**With Tours (`?withTours=true`)**

```json
{
  "data": [
    {
      "id": 1,
      "name": "Honeymoon",
      "tours": [
        {
          "id": 5,
          "title": "Maldives Couple Retreat",
          "destinationCity": "Malé",
          "destinationCountry": "Maldives",
          "status": "PUBLISHED"
        }
      ]
    }
  ]
}
```

#### Get Single Category by ID

```
GET /api/public/tour-categories/{id}
```

Always returns the category with its associated tour list.

---

### Admin Endpoints (Require Permission)

All admin category endpoints require an admin JWT and the appropriate permission.

#### Create Category

```
POST /api/admin/tour-categories
```

**Request Body**

```json
{
  "name": "Cultural Tour",
  "description": "Explore local heritage and traditions",
  "isActive": true
}
```

---

#### Update Category

```
PUT /api/admin/tour-categories/{id}
```

**Request Body** (same as create; all fields optional)

```json
{
  "name": "Cultural Tour",
  "isActive": false
}
```

---

#### Get Category by ID (Admin)

```
GET /api/admin/tour-categories/{id}
```

---

#### List All Categories (Admin)

```
GET /api/admin/tour-categories?activeOnly=true&withTours=false
```

| Param     | Type    | Default | Description                    |
|-----------|---------|---------|--------------------------------|
| activeOnly | boolean | null   | Filter to only active ones     |
| withTours | boolean | false   | Include associated tour list   |

---

#### Delete Category

```
DELETE /api/admin/tour-categories/{id}
```

> Returns `400` if the category is currently assigned to any tour packages.

---

## 5. Tour Package — Updated Fields

The tour package create/update request and all tour responses now include the following additional fields.

### New Request Fields

| Field       | Type       | Description                                           |
|-------------|------------|-------------------------------------------------------|
| flags       | `string[]` | `["POPULAR"]`, `["FEATURED"]`, or both, or empty      |
| categoryIds | `number[]` | IDs of `TourCategory` records to assign               |

**Example**

```json
{
  "title": "Sundarbans Wildlife Tour",
  "flags": ["POPULAR"],
  "categoryIds": [2, 5],
  "typeId": 1,
  "..."
}
```

### New Response Fields

| Field      | Type            | Description                                   |
|------------|-----------------|-----------------------------------------------|
| viewCount  | number          | Total number of detail page views             |
| flags      | `string[]`      | Assigned flags for this tour                  |
| categories | `Category[]`    | List of assigned categories (id, name, etc.)  |

**Category object in response**

```json
{
  "id": 2,
  "name": "Adventure",
  "description": "...",
  "isActive": true
}
```

---

## Available Categories (Reference)

| Name          |
|---------------|
| Honeymoon     |
| Adventure     |
| Family Tour   |
| Cultural Tour |
| Wildlife      |
| Beach Holiday |

> Categories are managed via admin panel. Use `GET /api/public/tour-categories` to fetch the live list.

---

## Available Flags (Reference)

| Flag     | Usage                                    |
|----------|------------------------------------------|
| POPULAR  | Tours with high engagement / search count |
| FEATURED | Editorially chosen tours to highlight    |

---

## Authentication Reference

| Endpoint Group               | Auth Required | Type         |
|------------------------------|---------------|--------------|
| `/api/public/**`             | No            | Public       |
| `/api/user/tour-favorites/**`| Yes           | User JWT     |
| `/api/admin/tour-categories/**` | Yes        | Admin JWT + Permission |
| `/api/tour-packages/**`      | Yes (write)   | Admin JWT + Permission |
