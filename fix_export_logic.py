
import os

path = '/Users/prashantkumar/AndroidStudioProjects/AudioMp3CutterRingtoneMaker/app/src/main/java/com/audio/mp3cutter/ringtone/maker/ui/export/ExportScreen.kt'

with open(path, 'r') as f:
    lines = f.readlines()

click_start = -1
click_end = -1

# Find clickable start (around line 410)
for i in range(400, 430):
    if '.clickable(' in lines[i] and 'enabled =' in lines[i+1]:
        # found parameters, look for {
        for j in range(i, i+10):
            if ') {' in lines[j]:
                click_start = j
                break
        break

# Find clickable end (around line 535) - looking for contentAlignment afterwards
for i in range(520, 560):
    if 'contentAlignment = Alignment.Center' in lines[i]:
        # The line before should be "}," or similar
        # scan backwards
        for j in range(i-1, i-5, -1):
            if '},' in lines[j] or '}' in lines[j]:
                click_end = j
                break
        break

if click_start != -1 and click_end != -1:
    print(f"Found clickable block: {click_start+1} to {click_end+1}")
    
    # Extract content
    # content is from click_start + 1 to click_end - 1
    # BUT, the closing brace of clickable is at click_end.
    # So content is up to click_end - 1.
    
    # We want to wrap everything in `val startExport = { ... }`
    # And then call it.
    
    # Get indentation from click_start content
    # lines[click_start+1] likely has indentation
    base_indent = lines[click_start+1][:len(lines[click_start+1]) - len(lines[click_start+1].lstrip())]
    
    # We need to preserve original indentation of the block? 
    # Or just wrap it?
    
    # Problem: wrapping adds a level of indentation. 
    # Kotlin is forgiving but it looks messy.
    
    # Let's just wrap it textually.
    
    original_content = lines[click_start+1 : click_end]
    
    new_content = []
    new_content.append(f"{base_indent}val activity = context as? android.app.Activity\n")
    new_content.append(f"{base_indent}val startExport = {{\n")
    new_content.extend(original_content) # Keep original lines
    new_content.append(f"{base_indent}}}\n\n")
    new_content.append(f"{base_indent}if (activity != null) {{\n")
    new_content.append(f"{base_indent}    com.audio.mp3cutter.ringtone.maker.ui.ads.InterstitialAdManager.showAd(activity) {{ startExport() }}\n")
    new_content.append(f"{base_indent}}} else {{\n")
    new_content.append(f"{base_indent}    startExport()\n")
    new_content.append(f"{base_indent}}}\n")
    
    # Replace lines
    lines[click_start+1 : click_end] = new_content
    
    with open(path, 'w') as f:
        f.writelines(lines)
    print("Patched ExportScreen successfully")
    
else:
    print("Could not find clickable block boundaries")
    print(f"Start: {click_start}, End: {click_end}")
