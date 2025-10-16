import os

# --- Configuration ---
OUTPUT_FILENAME = 'project_manifest.txt'

# Directories to completely exclude.
EXCLUDE_DIRS = {
    '.git', '.gradle', '.idea', 'build', '__pycache__'
}

# Specific files to exclude (sensitive info, build artifacts, this script).
EXCLUDE_FILES = {
    OUTPUT_FILENAME,
    'generate_manifest_for_review.py',
    'local.properties',
    'keystore.properties',
    'IntentionRepeater.jks',
    'gradle-wrapper.jar' # This is a binary, so we'll just list its path.
}

# File extensions to include the full source code for.
CODE_EXTENSIONS = {
    '.kt', '.kts', '.java', '.xml', '.pro', '.properties', '.toml'
}

# File extensions for which we will only list the path.
RESOURCE_EXTENSIONS = {
    '.png', '.webp', '.jar', '.bat'
}

# --- Header for the Next AI Session ---
DIRECTIONS_HEADER = """--- START OF DIRECTIONS FOR AI ---

Hello AI.

This file is a complete manifest of a modern Android application project. Your task is to act as an expert Android developer, and find why it crashes when the APK is run after being installed and provide the definitive solution to get the APK to fully run.

**Project Goal:**
A high-performance "Intention Repeater" app built with Kotlin, Jetpack Compose, MVVM architecture, and a Room database. It uses a Foreground Service to run reliably in the background.

**Current Status:**
The project is 99.9% complete. We have successfully configured the build environment, Gradle scripts, signing keys, and resources. However, we are stuck on a persistent crash when running the app after installing the APK.

**Your Task:**
1. Update my source code for error logging with timestamps to a intention_repeater_logfile.txt in an easy to get to place, and let me know where that will be stored on the phone.
2. It should run on a variety of Android phones. I want the min API while meeting Google's requirement and allowing full functionality.
3. Go through all the code and optimize for best practices, efficiency, UI/UX, memory management so it doesn't crash due to memory errors.
4. Thoroughly review all the source code files provided in this manifest.
5. Provide the final, corrected version of ANY file(s) that need to be changed into master_patch.txt.
6. Provide the exact, final sequence of commands to run from the command line to achieve a successful build of both the release APK and the AAB.
7. Put all changes into a master_patch.txt
8. Create a Python script master_patcher.py that applies the patch to the subfolders from the current folder.
9. Do away with Intention Multiplying (uses huge RAM) and replace with a burst option for the non-maximum speeds. This will fix memory errors.
    a. Burst option is an inner loop between each regular iteration. Default to 888888 times assigning the intention they gave to the intention variable.
    b. The burst number will be added to the iterations and the frequency for each iteration.
10. Program will now be v3.0 and build "70".

All that 1-10 code changes will go into two files you will produce:
1. master_patch.txt - Has all the changes made to files. Patch details.
2. master_patcher.py - The patcher that applies master_patch.txt to the project files.

Assume the build environment (JDK, Android SDK) is correctly set up. The problem lies within the code provided below.

--- END OF DIRECTIONS FOR AI ---

"""

def create_project_manifest():
    """
    Walks through the project directory and creates a manifest file
    containing the content of code files and paths of resource files.
    """
    print("Starting manifest generation for AI review...")
    file_count = 0
    
    try:
        with open(OUTPUT_FILENAME, 'w', encoding='utf-8', errors='ignore') as manifest_file:
            # Write the header first.
            manifest_file.write(DIRECTIONS_HEADER)
            
            # Walk from the current directory.
            for root, dirs, files in os.walk('.', topdown=True):
                # Prune excluded directories.
                dirs[:] = [d for d in sorted(dirs) if d not in EXCLUDE_DIRS]

                for filename in sorted(files):
                    if filename in EXCLUDE_FILES:
                        continue

                    file_path = os.path.join(root, filename)
                    relative_path = os.path.relpath(file_path).replace('\\', '/')
                    _, extension = os.path.splitext(filename)

                    manifest_file.write(f"--- FILE: {relative_path} ---\n")
                    
                    if extension.lower() in CODE_EXTENSIONS:
                        try:
                            with open(file_path, 'r', encoding='utf-8', errors='ignore') as source_file:
                                content = source_file.read()
                                manifest_file.write("--- CONTENT ---\n")
                                manifest_file.write(content)
                                manifest_file.write("\n--- END CONTENT ---\n")
                        except Exception as e:
                            manifest_file.write(f"[Error reading text file: {e}]\n")
                    elif extension.lower() in RESOURCE_EXTENSIONS:
                        manifest_file.write("[Resource File - Path Only]\n")
                    else:
                        manifest_file.write("[Other Non-Code File - Path Only]\n")

                    manifest_file.write(f"--- END FILE ---\n\n")
                    file_count += 1
                    print(f"  Processed: {relative_path}")

    except IOError as e:
        print(f"Error: Could not write to output file {OUTPUT_FILENAME}. Reason: {e}")
        return

    print(f"\nSuccess! Manifest created in '{OUTPUT_FILENAME}' with {file_count} files.")
    print("You can now copy the contents of this file to start a new session with AI.")

if __name__ == '__main__':
    create_project_manifest()