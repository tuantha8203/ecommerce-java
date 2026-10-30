package com.tuantha8203.shop.shared;

import java.util.List;

/** Cursor pagination result. nextCursor is null on the last page. */
public record CursorPage<T>(List<T> items, String nextCursor) {}
