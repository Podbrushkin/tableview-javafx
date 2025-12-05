
<img width="800" height="429" alt="Screenshot from 2025-12-05 12-46-25" src="https://github.com/user-attachments/assets/2e4338c0-4179-4513-a61d-9578167692b2" />

To use as Out-GridView in Powershell:
```powershell
# mvn clean compile package -q -e
. ./Out-GridViewFX.ps1
Get-Process | Out-GridViewFX -PassThru
```

```
Display given json as a table. Can display multiple tables in separate tabs. Can expand array of objects into subcolumns.

--in <path...>        # Path to json file or '-' for stdin. Json should be an array or map of arrays. Can provide multiple files for this key in separate args or in single arg delimited with a semicolon.
--pass-thru        # Window will have 'Submit' button, clicking it will print sequence numbers of selected items and exit. If input wasn't a plain array, will print items.
```

```powershell
# Build and run with Java 21+:
# sudo apt install openjdk-21-jdk
mvn clean compile package -q -e
java -jar ./target/javafx-tables-json-0.11-shaded.jar

# Try with your own data:
Get-Process | select id,ProcessName -f 100 | ConvertTo-Json > delme.json
gci -Recurse -File | select FullName,Length | ConvertTo-Json > delme2.json
java -jar .\target\javafx-tables-json-0.11-shaded.jar --in .\delme.json .\delme2.json

```
