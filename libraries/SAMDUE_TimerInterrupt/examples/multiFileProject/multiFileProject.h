/****************************************************************************************************************************
  multiFileProject.h
  For SAM DUE boards
  Written by Khoi Hoang

  Built by Khoi Hoang https://github.com/khoih-prog/SAMDUE_TimerInterrupt
  Licensed under MIT license

  Now even you use all these new 16 ISR-based timers,with their maximum interval practically unlimited (limited only by
  unsigned long miliseconds), you just consume only one SAMD timer and avoid conflicting with other cores' tasks.
  The accuracy is nearly perfect compared to software timers. The most important feature is they're ISR-based timers
  Therefore, their executions are not blocked by bad-behaving functions / tasks.
  This important feature is absolutely necessary for mission-critical tasks.
*****************************************************************************************************************************/

// To demo how to include files in multi-file Projects

#pragma once

// Can be included as many times as necessary, without `Multiple Definitions` Linker Error
#include "SAMDUETimerInterrupt.hpp"

// Can be included as many times as necessary, without `Multiple Definitions` Linker Error
#include "SAMDUE_ISR_Timer.hpp"
