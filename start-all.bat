@echo off
echo Starting all microservices...

start cmd /k "cd Service-Registry && mvnw.cmd spring-boot:run"
timeout /t 10

start cmd /k "cd Api-Gateway && mvnw.cmd spring-boot:run"
timeout /t 5

start cmd /k "cd Auth-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Profile-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Job-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Application-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Interview-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Notification-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Subscription-Service && mvnw.cmd spring-boot:run"
start cmd /k "cd Analytics-Service && mvnw.cmd spring-boot:run"

echo All services started!
pause