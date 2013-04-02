@echo off

set modName=%1
set modContainer=%2

set curdir=%CD%
set basedir=%curdir%\..

set srcFolder=%basedir%\%modContainer%\%modName%\src
set libFolder=%basedir%\%modContainer%\%modName%\class
set compilerFolder=%basedir%\compiler

set compilerReobfFolder=%compilerFolder%\reobf
set compilerSrcFolder=%compilerFolder%\src\minecraft

set reobfFolder=%srcFolder%\..\reobf

echo Cleaning org/..-ru-ee
del "%compilerSrcFolder%\org\apache\*" /s /f /q
del "%compilerSrcFolder%\org\bukkit\*" /s /f /q
del "%compilerSrcFolder%\org\yaml\*" /s /f /q
del "%compilerSrcFolder%\ru\*" /s /f /q
del "%compilerSrcFolder%\javassist\*" /s /f /q
del "%compilerSrcFolder%\ee\*" /s /f /q
del "%compilerSrcFolder%\railcraft\*" /s /f /q
del "%compilerSrcFolder%\net\minecraft\network\packet\IPacketHandler.java" /f /q

echo Cleaning compiler reobf client-server, mod-reobf
del "%compilerReobfFolder%\minecraft\*" /s /f /q
del "%reobfFolder%\*" /s /f /q

echo Deleting compiler src folders
rd "%compilerSrcFolder%\org\apache" /s /q
rd "%compilerSrcFolder%\org\bukkit" /s /q
rd "%compilerSrcFolder%\org\yaml" /s /q
rd "%compilerSrcFolder%\ru" /s /q
rd "%compilerSrcFolder%\ee" /s /q
rd "%compilerSrcFolder%\javassist" /s /q
rd "%compilerSrcFolder%\railcraft" /s /q

echo Deleting client-server folders
rd "%compilerReobfFolder%\minecraft" /s /q
rd "%reobfFolder%" /s /q

echo Copying files
xcopy "%srcFolder%\*" "%compilerSrcFolder%\" /s /q /i /y

echo Compiling
cd "%compilerFolder%"

runtime\bin\python\python_mcp runtime\recompile.py --client

echo Reobfuscating
runtime\bin\python\python_mcp runtime\reobfuscate.py --srgnames --client

rem echo Copying reobf lib
rem xcopy "%libFolder%\*" "%reobfFolder%\" /q /y /s /i
echo Copying reobf compiler/reobf
xcopy "%compilerReobfFolder%\minecraft\*" "%reobfFolder%\" /s /q /i /y
echo Copying reobf src base
xcopy "%srcFolder%\*" "%reobfFolder%\" /q /y


echo Running jar
cd "%reobfFolder%"
"C:\Program Files\Java\jdk1.7.0_09\bin\jar.exe" cf "%curdir%\mods\%modName%.jar" *

cd "%curdir%"
echo Done