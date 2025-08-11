#!/bin/bash

# اگر هر دستور خطا داد، اسکریپت متوقف شود
set -e

# پاکسازی بیلد قبلی و ساخت نسخه ریلیز
./gradlew clean assembleRelease

# انتشار پکیج
./gradlew publish