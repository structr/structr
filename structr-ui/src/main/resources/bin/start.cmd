for /f "delims=" %%x in (bin\config) do (set %%x)
java -cp ..\lib\*; %JAVA_OPTS% %MAIN_CLASS%