
import os

path = '/Users/prashantkumar/AndroidStudioProjects/AudioMp3CutterRingtoneMaker/app/src/main/java/com/audio/mp3cutter/ringtone/maker/ui/export/ExportScreen.kt'

with open(path, 'r') as f:
    lines = f.readlines()

found = False
for i, line in enumerate(lines):
    if 'contentAlignment = Alignment.Center' in line:
        found = True
        # Check previous line
        if i > 0 and '},' in lines[i-1]:
            # Replace }, with }
            # Use strict replacement to avoid replacing indentation
            lines[i-1] = lines[i-1].replace('},', '}')
            
            indent = " " * 52 # Approximate indentation (was 49/56?)
            # Adjust indent based on prev line?
            # prev line was "                                                 },"
            # It has 49 spaces.
            indent = lines[i-1].split('}')[0] # Grab indentation from line i-1
            
            code = [
                f"\n{indent}    if (activity != null) {{",
                f"{indent}        com.audio.mp3cutter.ringtone.maker.ui.ads.InterstitialAdManager.showAd(activity) {{",
                f"{indent}            startExport()",
                f"{indent}        }}",
                f"{indent}    }} else {{",
                f"{indent}        startExport()",
                f"{indent}    }}",
                f"{indent}}},\n"
            ]
            
            # Insert before current line (which is contentAlignment)
            # wait, lines.insert(i, ...) inserts BEFORE i.
            # Does lines[i-1] modification affect i? No.
            
            lines.insert(i, "\n".join(code))
            print("Patched file successfully")
            break

if found:
    with open(path, 'w') as f:
        f.writelines(lines)
else:
    print("Alignment not found")
