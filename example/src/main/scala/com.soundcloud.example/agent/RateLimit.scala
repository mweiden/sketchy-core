package com.soundcloud.example.agent

import java.util.Date

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.agent.RateLimiterAgent
import com.soundcloud.sketchy.agent.limits.Limits

import com.soundcloud.example.agent.limits.ExampleLimits


class ExampleRateLimiterAgent(
  counters: Context[Nothing],
  limits: Limits = new ExampleLimits())
  extends RateLimiterAgent(counters, limits)
