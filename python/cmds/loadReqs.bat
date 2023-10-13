:: auto-generates requirements and installs them to env
pip install pipreqs
pipreqs --force %projectFolderPath%\python
pip install -r %projectFolderPath%\python\requirements.txt

