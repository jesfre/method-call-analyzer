@echo off
set CURRENT_LOC=%CD%
set PROPERTY_FILE=%CD%\config.properties
set REPORT_ROOT=%CD%\reports
set CLASSPATH=%CD%\lib\*
set JAVA_OPTS= -Xms512m -Xmx1024m

echo Running class finder tool...

java -cp %CLASSPATH% -Xms512m -Xmx1024m com.blogspot.jesfre.classfinder.ClassFinder %CURRENT_LOC% %1

cd %CURRENT_LOC%
pause