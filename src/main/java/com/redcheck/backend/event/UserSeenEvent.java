package com.redcheck.backend.event;

import com.redcheck.backend.entity.User;

// Published by JwtAuthenticationFilter on every successfully authenticated
// request; UserActivityService decides whether it's worth a write. An event
// rather than a direct call so the filter (which every @WebMvcTest slice
// loads) doesn't gain a service dependency.
public record UserSeenEvent(User user) {}
