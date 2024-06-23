For developing in windows, if sync not working, then in the terminal, run:

```powershell
$env:PATH="I:\jdk-17.0.11+9\bin;C:\Users\river\.elan\toolchains\leanprover--lean4---v4.8.0-rc1\bin\;E:\anaconda3\Scripts\;E:\anaconda3\;$env:HOME\.elan\bin;$env:PATH"
$env:JAVA_HOME="I:\jdk-17.0.11+9"
./gradle.bat build
```

Or switch the 