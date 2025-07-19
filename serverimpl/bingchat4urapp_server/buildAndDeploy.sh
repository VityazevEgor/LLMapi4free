#!/bin/bash
cd ../../llmapi4free_lib
bash install.sh
cd ../serverimpl/bingchat4urapp_server/
# Execute Maven clean package command
mvn clean package -DskipTests

# Check if the command was successful
if [ $? -eq 0 ]; then
  echo "Команда выполнена успешно."
  
  # Define variables
  JAR_FILE=$(ls target/*.jar | head -n 1)  # Get the first .jar file in the target directory
  SERVER_USER="egor"
  SERVER_IP="5.182.86.164"
  DESTINATION_PATH="/home/egor/Downloads"  # Destination path on the server

  # Copy the .jar file to the server, replacing if it already exists
  rsync -avz "$JAR_FILE" "$SERVER_USER@$SERVER_IP:$DESTINATION_PATH"

  if [ $? -eq 0 ]; then
    echo "Файл .jar успешно скопирован на сервер в ~/Downloads."
  else
    echo "Произошла ошибка при копировании файла .jar на сервер."
  fi

else
  echo "Произошла ошибка при выполнении команды."
fi
