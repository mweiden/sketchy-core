package com.soundcloud.example.agent

import java.util.Date

import com.soundcloud.sketchy.context._
import com.soundcloud.sketchy.agent.RateLimiterAgent
import com.soundcloud.sketchy.agent.limits.BurstLimits

import com.soundcloud.example.agent.limits.ExampleBurstLimits


class ExampleRateLimiterAgent(
  counters: Context[Nothing],
  limits: BurstLimits = new ExampleBurstLimits())
  extends RateLimiterAgent(counters, limits)
