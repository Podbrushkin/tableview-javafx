function Out-GridViewFX {
    param (
        [Parameter(ValueFromPipeline = $true)]
			$InputObject,
		[switch]$PassThru
    )

    begin {
        [array]$items = @()
    }

    process {
        $items += $InputObject
    }

    end {
        if ($items.count -eq 0) {return}
		$formatData = Get-FormatData -TypeName ([array]$items)[0].GetType()
		$viewDef = $formatData.FormatViewDefinition | ? Control -is [System.Management.Automation.TableControl] | select -f 1

		# This one handles when pwsh has display hint for these objects
		$selectionMap = @()
        for ($i = 0; $i -lt $viewDef.Control.Headers.Label.Count; $i++) {
          $de = $viewDef.Control.Rows.Columns.DisplayEntry[$i]
          $isScript = $de.ValueType -eq 'ScriptBlock'
          $expr = $isScript ? [scriptblock]::Create($de.Value) : $de.Value

          if ($expr -eq 'NameString') {
            $expr = { ($_.NameString -replace '\u001b\[[0-9;]*m','') }
          }

          $label = $viewDef.Control.Headers.Label[$i] ?? $de.Value
          $selectionMap += @{Name=$label; Expression=$expr; }
        }
        $firstItem = ([array]$items)[0]

        # This one handles String and primitives
        if (!$selectionMap -and ($firstItem -is [ValueType] -or $firstItem -is [string])) {
          $selectionMap = @(@{Name='Value'; Expression={$_}})
        }

        # This is for all other objects
        if (!$selectionMap) {
          $selectionMap = $firstItem.psobject.Properties.name
        }

		$jsonStr = $items | Select-Object $selectionMap | ConvertTo-Json -Compress

        $javafxTableViewer = "$PSScriptRoot/target/javafx-tables-json-0.11-shaded.jar"
		$args = @()
		if ($PassThru) {$args += '--pass-thru'}
		$args += '--in','-'
		$args += '--title','Out-GridView'
		$args += '--dark'
		$argsStr = $args | % {"""$_"""} | Join-String -sep " "

        #$args = Write-Output --in - --title Out-GridView --dark

        $javaArgs = "-jar $javafxTableViewer $argsStr"

        # This one makes app 3x bigger in Ubuntu Gnome
        #$javaArgs = '-Dglass.gtk.uiScale=3 '+$javaArgs
        # This one makes app bigger in Windows maybe
        #$javaArgs = '-Dsun.java2d.uiScale=2.5 '+$javaArgs

        $processStartInfo = New-Object System.Diagnostics.ProcessStartInfo
        #$processStartInfo.FileName = "/usr/lib/jvm/java-21-openjdk-amd64/bin/java"
        $processStartInfo.FileName = "java"
        $processStartInfo.Arguments = $javaArgs
        $processStartInfo.RedirectStandardInput = $true
        $processStartInfo.RedirectStandardError = $true
        if ($PassThru) {
          $processStartInfo.RedirectStandardOutput = $true
        }
        $processStartInfo.UseShellExecute = $false
        $processStartInfo.CreateNoWindow = $true

        $process = [System.Diagnostics.Process]::Start($processStartInfo)
		#return $process
		if ($jsonStr) {
			# Write CSV to the Java process's STDIN
			$streamWriter = $process.StandardInput
			#foreach ($line in $csv) {
				$streamWriter.WriteLine($jsonStr)
			#}
			$streamWriter.Close()


			#write-host ($process.StandardOutput.ReadToEnd())
			#$process.WaitForExit()

			if ($PassThru) {
				# Wait for the Java process to exit
				$process.WaitForExit()

				# Read selected row indices from STDOUT
				$selectedIndices = $process.StandardOutput.ReadToEnd() -split "`r?`n" | ForEach-Object {
					if ($_ -match '^\d+$') { [int]$_ } else { $null }
				} | Where-Object { $_ -ne $null }

				# Return the selected objects
				if ($selectedIndices.count -gt 0) {
				 $items[$selectedIndices]
				}
			}

		}
    }
}
