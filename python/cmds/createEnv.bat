:: creates virtual environment "env"
IF exist %projectFolderPath%\python\env ( echo env exists ) ELSE ( py -m venv %projectFolderPath%\python\env && echo env created)