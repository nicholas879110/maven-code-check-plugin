@echo off
call %MAVEN_HOME%\bin\mvn  install:install-file -DgroupId=com.gome  -DartifactId=asm-all  -Dversion=1.0  -Dpackaging=jar  -Dfile=asm-all.jar
call %MAVEN_HOME%\bin\mvn  install:install-file -DgroupId=com.gome  -DartifactId=oromatcher  -Dversion=1.0  -Dpackaging=jar  -Dfile=oromatcher.jar
call %MAVEN_HOME%\bin\mvn  install:install-file -DgroupId=com.gome  -DartifactId=jgoodies-forms  -Dversion=1.0  -Dpackaging=jar  -Dfile=jgoodies-forms.jar
call %MAVEN_HOME%\bin\mvn  install:install-file -DgroupId=com.sun.java  -DartifactId=tools  -Dversion=1.0  -Dpackaging=jar  -Dfile=tools.jar
echo 'install ok!'
pause