/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "ABI8_0_0RCTI18nManager.h"
#import "ABI8_0_0RCTI18nUtil.h"

@implementation ABI8_0_0RCTI18nManager

ABI8_0_0RCT_EXPORT_MODULE()

ABI8_0_0RCT_EXPORT_METHOD(allowRTL:(BOOL)value)
{
  [[ABI8_0_0RCTI18nUtil sharedInstance] setAllowRTL:value];
}

- (NSDictionary *)constantsToExport
{
  return @{
    @"isRTL": @([[ABI8_0_0RCTI18nUtil sharedInstance] isRTL])
  };
}

@end
