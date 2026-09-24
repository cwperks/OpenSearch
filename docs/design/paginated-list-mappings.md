# Paginated index-mappings list API

## Status

Proposal. This document scopes the first implementation to index mappings. A paginated aliases API is a follow-up because alias selectors and missing-alias responses need separate API decisions.

## Problem

`GET /_mapping` and `GET /{index}/_mapping` return one mapping document for every matching index. On clusters with many indices or large mappings, the response can be too large to return, parse, and render efficiently.

Existing clients depend on the current mapping API response shape. Adding pagination metadata to that shape would require a conditional response schema and would complicate generated clients.

## Goals

- Provide cursor-based pagination over index mappings.
- Preserve the existing mapping APIs without behavioral or response-shape changes.
- Page only indices that are selected and authorized for the caller.
- Return each selected index's complete mapping in a page.
- Use a stateless cursor that is safe when indices are created or deleted between requests.
- Align request parameter names and response metadata with existing core pagination APIs.

## Non-goals

- Paginating fields inside one index mapping.
- Providing a cluster-state snapshot that remains immutable for the complete traversal.
- Changing the existing `/_mapping` or `/{index}/_mapping` APIs.
- Including alias pagination in the first implementation.

## Proposed REST API

```text
GET /_list/mappings
GET /_list/mappings/{index}
```

The endpoint accepts the existing mapping index selector and index options, plus these pagination parameters:

| Parameter | Description |
| --- | --- |
| `size` | Maximum number of index mapping entries in the response. Defaults to the list API default and is bounded by a server-defined maximum. |
| `sort` | `asc` or `desc`, ordered by concrete index name. Defaults to `asc`. |
| `next_token` | Opaque cursor from a previous response. |

Example:

```text
GET /_list/mappings?index=logs-*&size=100&sort=asc
```

Response:

```json
{
  "next_token": "<opaque token or null>",
  "mappings": {
    "logs-000001": {
      "mappings": {
        "properties": {
          "timestamp": { "type": "date" }
        }
      }
    },
    "logs-000002": {
      "mappings": {
        "properties": {
          "timestamp": { "type": "date" }
        }
      }
    }
  }
}
```

`size` counts index entries. It does not count fields or mapping bytes. The complete mapping for every returned index is included.

## Pagination semantics

The traversal order is lexicographic by concrete index name. The cursor contains the last returned index name and binds the token to:

- API kind (`list_mappings`)
- sort direction
- normalized index selector and relevant index options

The next page selects names greater than the cursor key for ascending order, or less than the cursor key for descending order.

This is a stateless traversal, not a point-in-time snapshot. Indices deleted after a page are absent from later pages. Indices created before the cursor position may not appear in the current traversal. Existing entries are neither duplicated nor returned out of order by a valid cursor.

Tokens must be validated before use. The implementation should use authenticated opaque tokens rather than treating Base64 encoding as a security mechanism.

## Authorization invariant

Pagination must occur after index selection and authorization:

```text
index expression and index options
→ concrete index resolution
→ Security authorization and request reduction
→ sort authorized concrete index names
→ apply cursor and size
→ retrieve mappings for the page
```

The token must be generated from the last index visible to the caller. A response must not include an unauthorized index, and a token must not encode an unauthorized index name.

This matches the model used by the Security collection-pagination proposal: process authorization and redaction before paginating. The core implementation and Security plugin integration tests must verify this invariant for both legacy and V4 privilege evaluation.

## Implementation shape

Use dedicated actions instead of adding optional pagination fields to `GetMappingsRequest` and `GetMappingsResponse`:

```text
ListMappingsAction
ListMappingsRequest
ListMappingsResponse
TransportListMappingsAction
RestListMappingsAction
```

Dedicated actions preserve the transport and REST contracts of the existing mapping APIs and allow a stable paginated response envelope.

The implementation can reuse `PageParams` and `PageToken` as common request and response metadata. It should not reuse `IndexPaginationStrategy` directly. That strategy orders all cluster metadata by creation time for `_list/indices`; mapping pagination needs lexicographic ordering over the already selected and authorized concrete indices.

Relevant existing seams:

```text
RestGetMappingAction
  → GetMappingsRequest
  → TransportGetMappingsAction
  → Metadata.findMappings(concreteIndices, fieldFilter)
```

The new transport action should resolve the same concrete indices as the existing mapping action, then paginate those concrete names before calling `Metadata.findMappings()` for the selected page.

## Error handling

Return `400 Bad Request` for:

- malformed or tampered `next_token`
- a token issued for another list endpoint
- a token whose sort direction does not match the request
- a token whose normalized selector or index options do not match the request
- invalid `size` or `sort`

The endpoint should retain normal mapping index-expression and `IndicesOptions` error behavior.

## Testing

### Unit tests

- ascending and descending lexicographic order
- multi-page traversal with no duplicates
- first, middle, and final page token behavior
- empty result
- maximum page size validation
- malformed token and token-binding validation
- additions and deletions between pages

### REST and transport tests

- existing `/_mapping` response remains unchanged
- `/_list/mappings` pages selected index mappings
- index wildcards and explicit index lists
- index option behavior for open, closed, hidden, and missing indices
- a page includes the full mapping for each selected index

### Security integration tests

Run both legacy and V4 privilege evaluation paths and verify:

- unauthorized indices are excluded before pagination
- page tokens contain only caller-visible index names
- page boundaries do not become sparse because unauthorized indices were selected before filtering
- a token obtained by a more privileged caller cannot cause a less privileged caller to receive an unauthorized mapping

## Follow-up: aliases

A later `/_list/aliases` API should also page by index entry and return the complete matching alias set for each page index. It needs additional design work because the current aliases API supports both index and alias selectors and has missing-explicit-alias behavior. Missing aliases must be computed from the complete authorized candidate set before slicing pages; computing them from one page can incorrectly return `404` when another requested alias appears on a later page.

## References

- [OpenSearch PR #14718: pagination for `_list/indices`](https://github.com/opensearch-project/OpenSearch/pull/14718)
- [OpenSearch Security PR #6378: pagination for collection APIs](https://github.com/opensearch-project/security/pull/6378)
