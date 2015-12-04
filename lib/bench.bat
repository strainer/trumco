"C:\Program Files\Java\jdk7\bin\javac.exe" -g transform.java
"C:\Program Files\Java\jdk7\bin\javac.exe" -g trumco.java

REM ~ java trumco sweep.wav -mrsmp 3 -pltht 310 -pltln 500 -logfs -minhz 100 -stspc 10000 -fnspc 29000
REM ~ java trumco sweep.wav -pltht 310 -pltln 500 -logfs -mrsmp 50 -stspc 10000 -fnspc 29000
REM ~ java trumco aphex.wav -minhz 500 -maxhz 5000 -mrsmp 1233 -gamma 0.7 -stspc 8000 -fnspc 13000
java trumco aphex.wav -logfs -mrsmp 1050 -minhz 48 -pltht 400 -pltln 400 -maxhz 15125

REM ~ start "C:\Program Files\IrfanView\i_view32.exe" sweep.0.bmp
start "C:\Program Files\IrfanView\i_view32.exe" aphex.0.bmp