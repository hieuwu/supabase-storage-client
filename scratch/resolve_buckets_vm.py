
import os

filepath = 'composeApp/src/commonMain/kotlin/com/hieuwu/supabasestorageclient/presentation/buckets/BucketsViewModel.kt'

with open(filepath, 'r') as f:
    lines = f.readlines()

start_index = -1
end_index = -1
for i, line in enumerate(lines):
    if 'init {' in line:
        start_index = i
    if start_index != -1 and 'fun refreshBuckets()' in line:
        end_index = i
        break

if start_index != -1 and end_index != -1:
    new_init = """    init {
        contextSelectionManager.clearContext()
        viewModelScope.launch {
            refreshManager.refreshBuckets.collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

"""
    new_lines = lines[:start_index] + [new_init] + lines[end_index:]
    with open(filepath, 'w') as f:
        f.writelines(new_lines)
    print("Successfully updated BucketsViewModel.kt")
else:
    print(f"Could not find function boundaries: {start_index}, {end_index}")
