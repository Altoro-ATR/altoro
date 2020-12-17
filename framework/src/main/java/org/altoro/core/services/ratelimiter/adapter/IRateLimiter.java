package org.altoro.core.services.ratelimiter.adapter;

import org.altoro.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
