
import os

filepath = 'composeApp/src/commonMain/kotlin/com/hieuwu/supabasestorageclient/presentation/main/MainViewModel.kt'

with open(filepath, 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if '<<<<<<< HEAD' in line:
        skip = True
        continue
    if '=======' in line:
        continue
    if '>>>>>>> main' in line:
        skip = False
        continue
    if not skip:
        new_lines.append(line)

# This just removes conflict markers by keeping the 'main' side or 'HEAD' side.
# But I want to merge them logically. 
# Actually, I'll just write the final version of the function I want.

start_index = -1
end_index = -1
for i, line in enumerate(lines):
    if 'fun onConfirmCreateBucket()' in line:
        start_index = i
    if start_index != -1 and 'fun onCreateFolder()' in line:
        end_index = i
        break

if start_index != -1 and end_index != -1:
    final_function = """    fun onConfirmCreateBucket() {
        val id = _uiState.value.newBucketId
        if (id.isBlank()) return

        val isSizeLimitEnabled = _uiState.value.isNewBucketSizeLimitEnabled
        val fileSizeLimit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeLimit.toLongOrNull() else null
        val isPublic = _uiState.value.isNewBucketPublic
        val unit = if (isSizeLimitEnabled) _uiState.value.newBucketFileSizeUnit else null

        viewModelScope.launch {
            if (_uiState.value.isUpdateBucketMode) {
                val params = UpdateBucketUseCase.Params(
                    id = id,
                    public = isPublic,
                    fileSizeLimit = fileSizeLimit,
                    unit = unit
                )
                updateBucketUseCase(params).fold(
                    onSuccess = {
                        val settings = observeUserSettingsUseCase().first()
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                isUpdateBucketMode = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                                newBucketFileSizeUnit = settings.fileSizeUnit,
                                successMessage = "Bucket updated"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                isUpdateBucketMode = false,
                                error = error.message
                            )
                        }
                    }
                )
            } else {
                val params = CreateBucketUseCase.Params(
                    id = id,
                    public = isPublic,
                    fileSizeLimit = fileSizeLimit,
                    unit = unit
                )
                createBucketUseCase(params).fold(
                    onSuccess = {
                        val settings = observeUserSettingsUseCase().first()
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                newBucketId = "",
                                isNewBucketPublic = true,
                                isNewBucketSizeLimitEnabled = false,
                                newBucketFileSizeLimit = settings.fileSizeLimit.toString(),
                                newBucketFileSizeUnit = settings.fileSizeUnit,
                                successMessage = "Bucket created"
                            )
                        }
                        refreshManager.triggerRefreshBuckets()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isCreateBucketDialogVisible = false,
                                error = error.message
                            )
                        }
                    }
                )
            }
        }
    }

"""
    new_lines = lines[:start_index] + [final_function] + lines[end_index:]
    with open(filepath, 'w') as f:
        f.writelines(new_lines)
    print("Successfully updated MainViewModel.kt")
else:
    print(f"Could not find function boundaries: {start_index}, {end_index}")
