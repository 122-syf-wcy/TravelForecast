<template>
  <div class="resource-container">
    <h1 class="text-2xl font-bold text-[#2C3E50] mb-6">资源管理</h1>
    
    <!-- 资源管理标签页 -->
    <div class="mb-6">
      <el-tabs v-model="resourceType" class="resource-tabs">
        <el-tab-pane label="景点管理" name="scenic">
          <div class="flex justify-between items-center mb-4">
            <div class="flex items-center">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索景点"
                class="w-64 mr-4"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              
              <el-select v-model="selectedTags" placeholder="标签筛选" class="w-64 mr-4" multiple collapse-tags>
                <el-option 
                  v-for="tag in availableTags" 
                  :key="tag" 
                  :label="tag" 
                  :value="tag" 
                />
              </el-select>
              
              <el-button text @click="clearFilters">
                清空筛选
              </el-button>
            </div>
            
            <el-button v-if="resourceType !== 'scenic'" type="primary" :icon="Plus" @click="handleAdd">
              {{ resourceType === 'activity' ? '添加活动' : '添加设施' }}
            </el-button>
            <el-tooltip v-else content="景区由管理员统一管理" placement="top">
              <el-button type="info" :icon="Plus" disabled>
                添加景点
              </el-button>
            </el-tooltip>
          </div>
          
          <holographic-card>
            <div class="p-4">
              <el-table :data="currentResources" style="width: 100%" :header-cell-style="{ background: 'transparent', color: '#606266' }">
                <el-table-column prop="name" label="景点名称">
                  <template #default="{ row }">
                    <div class="flex items-center">
                      <el-image :src="row.thumbnail" class="w-12 h-12 rounded mr-3" fit="cover" />
                      <div>
                        <div class="text-gray-800">{{ row.name }}</div>
                        <div class="text-xs text-gray-500">ID: {{ row.id }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="tags" label="标签" width="200">
                  <template #default="{ row }">
                    <div class="flex flex-wrap gap-1">
                      <el-tag 
                        v-for="tag in row.tags" 
                        :key="tag" 
                        size="small" 
                        effect="dark"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="visitCount" label="访问量" width="120" sortable />
                
                <el-table-column prop="rating" label="评分" width="120" sortable>
                  <template #default="{ row }">
                    <div class="flex items-center">
                      <span class="mr-1">{{ row.rating }}</span>
                      <el-rate v-model="row.rating" disabled text-color="#F9F9F9" :max="5" />
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="status" label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" effect="dark">
                      {{ getStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column prop="updateTime" label="更新时间" width="120" sortable />
                
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <div class="flex space-x-2">
                      <el-button size="small" type="primary" text @click="handleEdit(row)">编辑</el-button>
                      <el-button size="small" type="success" text @click="handleUploadImage(row)">上传图片</el-button>
                      <el-button size="small" type="danger" text @click="handleDelete(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </holographic-card>
        </el-tab-pane>
        
        <el-tab-pane label="子景点管理" name="subspot">
          <div class="flex justify-between items-center mb-4">
            <div class="flex items-center">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索子景点"
                class="w-64 mr-4"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            
            <el-button type="primary" :icon="Plus" @click="handleAddSubSpot">
              添加子景点
            </el-button>
          </div>
          
          <holographic-card>
            <div class="p-4">
              <el-empty v-if="subSpotResources.length === 0" description="暂无子景点，点击右上角添加" />
              <el-table v-else :data="filteredSubSpots" style="width: 100%" :header-cell-style="{ background: 'transparent', color: '#606266' }">
                <el-table-column prop="name" label="子景点名称">
                  <template #default="{ row }">
                    <div class="flex items-center">
                      <el-image v-if="row.imageUrl" :src="row.imageUrl" class="w-12 h-12 rounded mr-3" fit="cover" />
                      <el-icon v-else class="w-12 h-12 mr-3 text-gray-300" :size="32"><Picture /></el-icon>
                      <div>
                        <div class="text-gray-800">{{ row.name }}</div>
                        <div class="text-xs text-gray-500">{{ row.description }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="tags" label="标签" width="200">
                  <template #default="{ row }">
                    <div class="flex flex-wrap gap-1">
                      <el-tag 
                        v-for="tag in (row.parsedTags || [])" 
                        :key="tag" 
                        size="small" 
                        effect="dark"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="openingHours" label="开放时间" width="150" />
                
                <el-table-column prop="sortOrder" label="排序" width="80" sortable />
                
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status?.toLowerCase())" effect="dark">
                      {{ getStatusText(row.status?.toLowerCase()) }}
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column label="操作" width="180">
                  <template #default="{ row }">
                    <div class="flex space-x-2">
                      <el-button size="small" type="primary" text @click="handleEditSubSpot(row)">编辑</el-button>
                      <el-button size="small" type="danger" text @click="handleDeleteSubSpot(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </holographic-card>
        </el-tab-pane>
        
        <el-tab-pane label="活动管理" name="activity">
          <div class="flex justify-between items-center mb-4">
            <div class="flex items-center">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索活动"
                class="w-64 mr-4"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              
              <el-select v-model="selectedTags" placeholder="标签筛选" class="w-64 mr-4" multiple collapse-tags>
                <el-option 
                  v-for="tag in availableTags" 
                  :key="tag" 
                  :label="tag" 
                  :value="tag" 
                />
              </el-select>
              
              <el-button text @click="clearFilters">
                清空筛选
              </el-button>
            </div>
            
            <el-button type="primary" :icon="Plus" @click="handleAdd">
              添加活动
            </el-button>
          </div>
          
          <holographic-card>
            <div class="p-4">
              <el-table :data="currentResources" style="width: 100%" :header-cell-style="{ background: 'transparent', color: '#606266' }">
                <el-table-column prop="name" label="活动名称">
                  <template #default="{ row }">
                    <div class="flex items-center">
                      <el-image :src="row.thumbnail" class="w-12 h-12 rounded mr-3" fit="cover" />
                      <div>
                        <div class="text-gray-800">{{ row.name }}</div>
                        <div class="text-xs text-gray-500">ID: {{ row.id }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="tags" label="标签" width="200">
                  <template #default="{ row }">
                    <div class="flex flex-wrap gap-1">
                      <el-tag 
                        v-for="tag in row.tags" 
                        :key="tag" 
                        size="small" 
                        effect="dark"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column label="活动时间" width="200">
                  <template #default="{ row }">
                    <div>
                      <div>开始: {{ row.startTime }}</div>
                      <div>结束: {{ row.endTime }}</div>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="visitCount" label="参与人数" width="120" sortable />
                
                <el-table-column prop="status" label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" effect="dark">
                      {{ getStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column label="推广状态" width="100">
                  <template #default="{ row }">
                    <el-tag v-if="row.isPromoted" type="success" effect="dark">
                      已推广
                    </el-tag>
                    <el-tag v-else type="info" effect="dark">
                      未推广
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column prop="updateTime" label="更新时间" width="120" sortable />
                
                <el-table-column label="操作" width="250">
                  <template #default="{ row }">
                    <div class="flex space-x-2">
                      <el-button size="small" type="primary" text @click="handleEdit(row)">编辑</el-button>
                      <el-button size="small" type="success" text @click="handleUploadImage(row)">上传图片</el-button>
                      <el-button 
                        size="small" 
                        :type="row.isPromoted ? 'info' : 'warning'" 
                        text 
                        @click="handlePromote(row)"
                        :disabled="row.isPromoted"
                      >
                        {{ row.isPromoted ? '已推广' : '推广' }}
                      </el-button>
                      <el-button size="small" type="danger" text @click="handleDelete(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </holographic-card>
        </el-tab-pane>
        
        <el-tab-pane label="设施管理" name="facility">
          <div class="flex justify-between items-center mb-4">
            <div class="flex items-center">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索设施"
                class="w-64 mr-4"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              
              <el-select v-model="selectedTags" placeholder="标签筛选" class="w-64 mr-4" multiple collapse-tags>
                <el-option 
                  v-for="tag in availableTags" 
                  :key="tag" 
                  :label="tag" 
                  :value="tag" 
                />
              </el-select>
              
              <el-button text @click="clearFilters">
                清空筛选
              </el-button>
            </div>
            
            <el-button type="primary" :icon="Plus" @click="handleAdd">
              添加设施
            </el-button>
          </div>
          
          <holographic-card>
            <div class="p-4">
              <el-table :data="currentResources" style="width: 100%" :header-cell-style="{ background: 'transparent', color: '#606266' }">
                <el-table-column prop="name" label="设施名称">
                  <template #default="{ row }">
                    <div class="flex items-center">
                      <el-image :src="row.thumbnail" class="w-12 h-12 rounded mr-3" fit="cover" />
                      <div>
                        <div class="text-gray-800">{{ row.name }}</div>
                        <div class="text-xs text-gray-500">ID: {{ row.id }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="tags" label="标签" width="200">
                  <template #default="{ row }">
                    <div class="flex flex-wrap gap-1">
                      <el-tag 
                        v-for="tag in row.tags" 
                        :key="tag" 
                        size="small" 
                        effect="dark"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="capacity" label="容量" width="100" sortable />
                
                <el-table-column prop="openTime" label="开放时间" width="150" />
                
                <el-table-column prop="status" label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" effect="dark">
                      {{ getStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column prop="updateTime" label="更新时间" width="120" sortable />
                
                <el-table-column label="操作" width="250">
                  <template #default="{ row }">
                    <div class="flex space-x-2">
                      <el-button size="small" type="primary" text @click="handleEdit(row)">编辑</el-button>
                      <el-button size="small" type="success" text @click="handleUploadImage(row)">上传图片</el-button>
                      <el-button size="small" type="warning" text @click="handleMaintenance(row)">维护</el-button>
                      <el-button size="small" type="danger" text @click="handleDelete(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </holographic-card>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 景点编辑/添加对话框 -->
    <el-dialog 
      v-model="scenicDialogVisible" 
      :title="isEditMode ? '编辑景点' : '添加景点'"
      width="600px"
    >
      <el-form :model="scenicForm" :rules="scenicRules" ref="scenicFormRef" label-width="100px">
        <el-form-item label="景点名称" prop="name">
          <el-input v-model="scenicForm.name" placeholder="请输入景点名称" />
        </el-form-item>
        
        <el-form-item label="景点描述" prop="description">
          <el-input 
            v-model="scenicForm.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入景点描述"
          />
        </el-form-item>
        
        <el-form-item label="缩略图URL" prop="thumbnail">
          <el-input v-model="scenicForm.thumbnail" placeholder="请输入图片URL或上传图片" />
        </el-form-item>
        
        <el-form-item label="景点标签" prop="tags">
          <el-select v-model="scenicForm.tags" multiple placeholder="请选择标签" style="width: 100%">
            <el-option 
              v-for="tag in allTags.scenic" 
              :key="tag" 
              :label="tag" 
              :value="tag" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="scenicForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="maintenance">维护中</el-radio>
            <el-radio value="inactive">已停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="scenicDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitScenicForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 活动编辑/添加对话框 -->
    <el-dialog 
      v-model="activityDialogVisible" 
      :title="isEditMode ? '编辑活动' : '添加活动'"
      width="600px"
    >
      <el-form :model="activityForm" :rules="activityRules" ref="activityFormRef" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="activityForm.name" placeholder="请输入活动名称" />
        </el-form-item>
        
        <el-form-item label="活动描述" prop="description">
          <el-input 
            v-model="activityForm.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>
        
        <el-form-item label="封面图片" prop="coverImage">
          <div class="flex items-center space-x-4">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleActivityImageUpload"
              :before-upload="beforeImageUpload"
              accept="image/*"
            >
              <img v-if="activityForm.coverImage" :src="activityForm.coverImage" class="avatar" style="width: 178px; height: 178px; object-fit: cover;" />
              <el-icon v-else class="avatar-uploader-icon" style="width: 178px; height: 178px; line-height: 178px; font-size: 28px; color: #8c939d; border: 1px dashed #d9d9d9; text-align: center;">
                <Plus />
              </el-icon>
            </el-upload>
            <div class="text-xs text-gray-500">
              <p>支持 jpg/png/webp 格式</p>
              <p>图片大小不超过 2MB</p>
              <p>建议尺寸: 800x600</p>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="活动标签" prop="tags">
          <el-select v-model="activityForm.tags" multiple placeholder="请选择标签" style="width: 100%">
            <el-option 
              v-for="tag in allTags.activity" 
              :key="tag" 
              :label="tag" 
              :value="tag" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker 
            v-model="activityForm.startTime" 
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker 
            v-model="activityForm.endTime" 
            type="date"
            placeholder="选择结束日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="activityForm.status">
            <el-radio value="upcoming">即将开始</el-radio>
            <el-radio value="active">进行中</el-radio>
            <el-radio value="ended">已结束</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="activityDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitActivityForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 图片上传对话框 -->
    <el-dialog 
      v-model="uploadDialogVisible" 
      :title="getUploadDialogTitle()"
      width="600px"
    >
      <el-form label-width="100px">
        <el-form-item :label="getResourceLabel()">
          <el-input v-model="currentUploadScenic.name" disabled />
        </el-form-item>
        
        <el-form-item label="资源ID">
          <el-input v-model="currentUploadScenic.id" disabled />
        </el-form-item>
        
        <el-form-item label="图片类型" v-if="resourceType === 'scenic'">
          <el-select v-model="uploadImageType" placeholder="请选择图片类型" style="width: 100%">
            <el-option label="封面图" value="COVER" />
            <el-option label="画廊图" value="GALLERY" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择图片">
          <el-upload
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="uploadFileList"
            :limit="1"
            accept="image/*"
            list-type="picture"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip text-gray-500">
                支持 jpg/png/webp 格式，单张图片大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpload">确定上传</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 设施编辑/添加对话框 -->
    <el-dialog 
      v-model="facilityDialogVisible" 
      :title="isEditMode ? '编辑设施' : '添加设施'"
      width="600px"
    >
      <el-form :model="facilityForm" :rules="facilityRules" ref="facilityFormRef" label-width="100px">
        <el-form-item label="设施名称" prop="name">
          <el-input v-model="facilityForm.name" placeholder="请输入设施名称" />
        </el-form-item>
        
        <el-form-item label="设施描述" prop="description">
          <el-input 
            v-model="facilityForm.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入设施描述"
          />
        </el-form-item>
        
        <el-form-item label="设施图片" prop="thumbnail">
          <div class="flex items-center space-x-4">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleFacilityImageUpload"
              :before-upload="beforeImageUpload"
              accept="image/*"
            >
              <img v-if="facilityForm.thumbnail" :src="facilityForm.thumbnail" class="avatar" style="width: 178px; height: 178px; object-fit: cover;" />
              <el-icon v-else class="avatar-uploader-icon" style="width: 178px; height: 178px; line-height: 178px; font-size: 28px; color: #8c939d; border: 1px dashed #d9d9d9; text-align: center;">
                <Plus />
              </el-icon>
            </el-upload>
            <div class="text-xs text-gray-500">
              <p>支持 jpg/png/webp 格式</p>
              <p>图片大小不超过 2MB</p>
              <p>建议尺寸: 800x600</p>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="设施标签" prop="tags">
          <el-select v-model="facilityForm.tags" multiple placeholder="请选择标签" style="width: 100%">
            <el-option 
              v-for="tag in allTags.facility" 
              :key="tag" 
              :label="tag" 
              :value="tag" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="facilityForm.capacity" :min="1" placeholder="请输入容量" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="开放时间" prop="openTime">
          <el-input v-model="facilityForm.openTime" placeholder="例如: 08:00-18:00 或 全天开放" />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="facilityForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="maintenance">维护中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="facilityDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitFacilityForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 子景点编辑/添加对话框 -->
    <el-dialog 
      v-model="subSpotDialogVisible" 
      :title="isEditMode ? '编辑子景点' : '添加子景点'"
      width="600px"
    >
      <el-form :model="subSpotForm" :rules="subSpotRules" ref="subSpotFormRef" label-width="100px">
        <el-form-item label="景点名称" prop="name">
          <el-input v-model="subSpotForm.name" placeholder="如：滑雪场、观景台、休息区" />
        </el-form-item>
        
        <el-form-item label="景点描述" prop="description">
          <el-input 
            v-model="subSpotForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入子景点简介"
          />
        </el-form-item>
        
        <el-form-item label="景点图片" prop="imageUrl">
          <div class="flex items-center space-x-4">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleSubSpotImageUpload"
              :before-upload="beforeImageUpload"
              accept="image/*"
            >
              <img v-if="subSpotForm.imageUrl" :src="subSpotForm.imageUrl" class="avatar" style="width: 178px; height: 178px; object-fit: cover;" />
              <el-icon v-else class="avatar-uploader-icon" style="width: 178px; height: 178px; line-height: 178px; font-size: 28px; color: #8c939d; border: 1px dashed #d9d9d9; text-align: center;">
                <Plus />
              </el-icon>
            </el-upload>
            <div class="text-xs text-gray-500">
              <p>支持 jpg/png/webp 格式</p>
              <p>图片大小不超过 2MB</p>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="景点标签" prop="tags">
          <el-select v-model="subSpotForm.tags" multiple placeholder="请选择标签" style="width: 100%">
            <el-option 
              v-for="tag in allTags.scenic" 
              :key="tag" 
              :label="tag" 
              :value="tag" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="开放时间">
          <el-input v-model="subSpotForm.openingHours" placeholder="例如: 08:00-18:00 或 全天开放" />
        </el-form-item>
        
        <el-form-item label="排序序号">
          <el-input-number v-model="subSpotForm.sortOrder" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="subSpotDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSubSpotForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Plus, UploadFilled, Picture } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// 获取 Token
const getToken = () => {
  return localStorage.getItem('token') || ''
}

// 当前选中的标签页
const resourceType = ref('scenic')

// 搜索关键词
const searchKeyword = ref('')

// 资源标签筛选
const selectedTags = ref([])

// 所有可用标签
const allTags = {
  scenic: ['自然风光', '人文景观', '历史古迹', '休闲娱乐', '特色建筑', '网红打卡', '亲子游玩'],
  activity: ['节日活动', '文化表演', '体育赛事', '特色体验', '季节限定', '亲子活动', '团建活动'],
  facility: ['餐饮设施', '休息区', '卫生间', '停车场', '售票处', '导览设施', '安全设施']
}

// 根据当前选择的资源类型获取对应的标签
const availableTags = computed(() => {
  return allTags[resourceType.value as keyof typeof allTags] || []
})

// 根据当前选择的资源类型获取对应的资源数据
const currentResources = computed(() => {
  if (resourceType.value === 'scenic') {
    return filterResources(scenicResources.value)
  } else if (resourceType.value === 'activity') {
    return filterResources(activityResources.value)
  } else {
    return filterResources(facilityResources.value)
  }
})

// 根据搜索关键词和标签筛选资源
const filterResources = (resources: any[]) => {
  if (!searchKeyword.value && selectedTags.value.length === 0) {
    return resources
  }
  
  return resources.filter((resource: any) => {
    // 关键词筛选
    const matchKeyword = !searchKeyword.value || 
      resource.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      resource.description.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      resource.id.toLowerCase().includes(searchKeyword.value.toLowerCase())
    
    // 标签筛选
    const matchTags = selectedTags.value.length === 0 || 
      selectedTags.value.some(tag => resource.tags.includes(tag))
    
    return matchKeyword && matchTags
  })
}

// 获取资源状态对应的样式
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    active: 'success',
    maintenance: 'warning',
    inactive: 'info',
    upcoming: 'primary',
    ended: 'info'
  }
  return statusMap[status] || 'info'
}

// 获取资源状态对应的文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    active: '正常',
    maintenance: '维护中',
    inactive: '已停用',
    upcoming: '即将开始',
    ended: '已结束'
  }
  return statusMap[status] || '未知'
}

// 删除资源
const handleDelete = async (resource: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资源"${resource.name}"吗？此操作不可逆。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用API删除资源
    if (resourceType.value === 'scenic') {
      // 景区由管理员管理，商家无权删除
      ElMessage.warning('景区删除需联系管理员操作')
    } else if (resourceType.value === 'activity') {
      // 调用活动删除API
      const { deleteActivity } = await import('@/api/resource')
      await deleteActivity(resource.id)
      ElMessage.success('活动删除成功')
      // 重新加载活动列表
      await loadActivities()
    } else {
      // 调用设施删除API
      const { deleteFacility } = await import('@/api/resource')
      await deleteFacility(resource.id)
      ElMessage.success('设施删除成功')
      // 重新加载设施列表
      await loadFacilities()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

// 对话框可见性
const scenicDialogVisible = ref(false)
const activityDialogVisible = ref(false)
const facilityDialogVisible = ref(false)
const uploadDialogVisible = ref(false)

// 是否为编辑模式
const isEditMode = ref(false)
const currentEditId = ref('')

// 上传图片相关
const currentUploadScenic = ref({
  id: '',
  name: ''
})
const uploadFileList = ref<any[]>([])
const imageUrl = ref('')
const uploadImageType = ref('COVER') // 默认为封面图

// 表单引用
const scenicFormRef = ref()
const activityFormRef = ref()
const facilityFormRef = ref()

// 景点表单数据
const scenicForm = ref({
  name: '',
  description: '',
  thumbnail: '',
  tags: [] as string[],
  status: 'active'
})

// 活动表单数据
const activityForm = ref({
  name: '',
  description: '',
  coverImage: '',
  tags: [] as string[],
  startTime: '',
  endTime: '',
  status: 'upcoming'
})

// 设施表单数据
const facilityForm = ref({
  name: '',
  description: '',
  thumbnail: '',
  tags: [] as string[],
  capacity: 100,
  openTime: '08:00-18:00',
  status: 'active'
})

// 表单验证规则
const scenicRules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入景点描述', trigger: 'blur' }],
  tags: [{ required: true, message: '请选择至少一个标签', trigger: 'change' }]
}

const activityRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  tags: [{ required: true, message: '请选择至少一个标签', trigger: 'change' }]
}

const facilityRules = {
  name: [{ required: true, message: '请输入设施名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入设施描述', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }],
  openTime: [{ required: true, message: '请输入开放时间', trigger: 'blur' }],
  tags: [{ required: true, message: '请选择至少一个标签', trigger: 'change' }]
}

// 添加资源
const handleAdd = () => {
  isEditMode.value = false
  currentEditId.value = ''
  
  // 根据资源类型打开对应的对话框并重置表单
  if (resourceType.value === 'scenic') {
    scenicForm.value = {
      name: '',
      description: '',
      thumbnail: '',
      tags: [],
      status: 'active'
    }
    scenicDialogVisible.value = true
  } else if (resourceType.value === 'activity') {
    activityForm.value = {
      name: '',
      description: '',
      thumbnail: '',
      tags: [],
      startTime: '',
      endTime: '',
      status: 'upcoming'
    }
    activityDialogVisible.value = true
  } else {
    facilityForm.value = {
      name: '',
      description: '',
      thumbnail: '',
      tags: [],
      capacity: 100,
      openTime: '08:00-18:00',
      status: 'active'
    }
    facilityDialogVisible.value = true
  }
}

// 编辑资源
const handleEdit = (resource: any) => {
  isEditMode.value = true
  currentEditId.value = resource.id
  
  // 根据资源类型打开对应的对话框并填充数据
  if (resourceType.value === 'scenic') {
    scenicForm.value = {
      name: resource.name,
      description: resource.description,
      thumbnail: resource.thumbnail,
      tags: [...resource.tags],
      status: resource.status
    }
    scenicDialogVisible.value = true
  } else if (resourceType.value === 'activity') {
    activityForm.value = {
      name: resource.name,
      description: resource.description,
      thumbnail: resource.thumbnail,
      tags: [...resource.tags],
      startTime: resource.startTime,
      endTime: resource.endTime,
      status: resource.status
    }
    activityDialogVisible.value = true
  } else {
    facilityForm.value = {
      name: resource.name,
      description: resource.description,
      thumbnail: resource.thumbnail,
      tags: [...resource.tags],
      capacity: resource.capacity,
      openTime: resource.openTime,
      status: resource.status
    }
    facilityDialogVisible.value = true
  }
}

// 提交景点表单
const submitScenicForm = () => {
  scenicFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (isEditMode.value) {
        // 编辑模式
        const index = scenicResources.value.findIndex(item => item.id === currentEditId.value)
        if (index !== -1) {
          scenicResources.value[index] = {
            ...scenicResources.value[index],
            ...scenicForm.value,
            updateTime: new Date().toISOString().split('T')[0]
          }
          // 调用后端API更新景区
          try {
            const { updateScenic } = await import('@/api/scenic')
            await updateScenic(currentEditId.value, scenicForm.value)
            ElMessage.success('景点更新成功')
          } catch (apiError) {
            console.error('景点更新API调用失败:', apiError)
            ElMessage.success('景点已本地更新（后端同步失败）')
          }
        }
      } else {
        // 新增模式
        const newScenic = {
          id: `SC${String(scenicResources.value.length + 1).padStart(3, '0')}`,
          ...scenicForm.value,
          visitCount: 0,
          rating: 0,
          createTime: new Date().toISOString().split('T')[0],
          updateTime: new Date().toISOString().split('T')[0]
        }
        scenicResources.value.push(newScenic)
        // 景区新增由管理员操作，商家端不支持
        ElMessage.warning('新增景区需联系管理员操作')
        scenicResources.value.pop() // 回退本地添加
      }
      scenicDialogVisible.value = false
    }
  })
}

// 提交活动表单
const submitActivityForm = async () => {
  activityFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (isEditMode.value) {
          // 编辑模式
          const { updateActivity } = await import('@/api/resource')
          await updateActivity(currentEditId.value, {
            name: activityForm.value.name,
            description: activityForm.value.description,
            coverImage: activityForm.value.coverImage,
            tags: JSON.stringify(activityForm.value.tags),
            startTime: activityForm.value.startTime,
            endTime: activityForm.value.endTime,
            status: activityForm.value.status
          })
          ElMessage.success('活动更新成功')
          // 重新加载活动列表
          await loadActivities()
        } else {
          // 新增模式
          const { createActivity } = await import('@/api/resource')
          await createActivity({
            name: activityForm.value.name,
            description: activityForm.value.description,
            coverImage: activityForm.value.coverImage,
            tags: JSON.stringify(activityForm.value.tags),
            startTime: activityForm.value.startTime,
            endTime: activityForm.value.endTime,
            status: activityForm.value.status
          })
          ElMessage.success('活动添加成功')
          // 重新加载活动列表
          await loadActivities()
        }
        activityDialogVisible.value = false
      } catch (error: any) {
        console.error('活动操作失败:', error)
        ElMessage.error(error.message || '操作失败，请重试')
      }
    }
  })
}

// 提交设施表单
const submitFacilityForm = async () => {
  facilityFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (isEditMode.value) {
          // 编辑模式
          const { updateFacility } = await import('@/api/resource')
          await updateFacility(currentEditId.value, {
            name: facilityForm.value.name,
            description: facilityForm.value.description,
            thumbnail: facilityForm.value.thumbnail,
            features: JSON.stringify(facilityForm.value.tags),
            capacity: facilityForm.value.capacity,
            openingHours: facilityForm.value.openTime,
            status: facilityForm.value.status
          })
          ElMessage.success('设施更新成功')
          // 重新加载设施列表
          await loadFacilities()
        } else {
          // 新增模式
          const { createFacility } = await import('@/api/resource')
          await createFacility({
            name: facilityForm.value.name,
            description: facilityForm.value.description,
            thumbnail: facilityForm.value.thumbnail,
            features: JSON.stringify(facilityForm.value.tags),
            capacity: facilityForm.value.capacity,
            openingHours: facilityForm.value.openTime,
            status: facilityForm.value.status
          })
          ElMessage.success('设施添加成功')
          // 重新加载设施列表
          await loadFacilities()
        }
        facilityDialogVisible.value = false
      } catch (error: any) {
        console.error('设施操作失败:', error)
        ElMessage.error(error.message || '操作失败，请重试')
      }
    }
  })
}

// 上传图片（景点管理）
const handleUploadImage = (resource: any) => {
  currentUploadScenic.value = {
    id: resource.id,
    name: resource.name
  }
  uploadFileList.value = []
  imageUrl.value = ''
  uploadImageType.value = 'COVER' // 默认为封面图（仅用于景点）
  uploadDialogVisible.value = true
}

// 获取上传对话框标题
const getUploadDialogTitle = () => {
  if (resourceType.value === 'scenic') return '上传景点图片'
  if (resourceType.value === 'activity') return '上传活动图片'
  if (resourceType.value === 'facility') return '上传设施图片'
  return '上传图片'
}

// 获取资源标签
const getResourceLabel = () => {
  if (resourceType.value === 'scenic') return '景点名称'
  if (resourceType.value === 'activity') return '活动名称'
  if (resourceType.value === 'facility') return '设施名称'
  return '资源名称'
}

// 活动图片上传前验证
const beforeImageUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 活动图片上传处理
const handleActivityImageUpload = async (options: any) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const token = getToken()
    const response = await axios.post('/api/oss/upload', formData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data.code === 1 || response.data.code === 200) {
      const imageUrl = response.data.data?.url || response.data.data
      activityForm.value.coverImage = imageUrl
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(response.data.msg || '图片上传失败')
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    ElMessage.error(error.message || '图片上传失败，请重试')
  }
}

// 设施图片上传处理
const handleFacilityImageUpload = async (options: any) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const token = getToken()
    const response = await axios.post('/api/oss/upload', formData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data.code === 1 || response.data.code === 200) {
      const imageUrl = response.data.data?.url || response.data.data
      facilityForm.value.thumbnail = imageUrl
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(response.data.msg || '图片上传失败')
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    ElMessage.error(error.message || '图片上传失败，请重试')
  }
}

// 处理文件选择
const handleFileChange = (file: any, fileList: any[]) => {
  // 验证文件大小（与管理员端保持一致：2MB）
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
    // 移除不符合要求的文件
    const index = fileList.findIndex(item => item.uid === file.uid)
    if (index > -1) {
      fileList.splice(index, 1)
    }
    return
  }
  
  // 验证文件类型
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    const index = fileList.findIndex(item => item.uid === file.uid)
    if (index > -1) {
      fileList.splice(index, 1)
    }
    return
  }
  
  uploadFileList.value = fileList
}

// 添加URL图片
const addImageUrl = () => {
  if (!imageUrl.value) {
    ElMessage.warning('请输入图片URL')
    return
  }
  
  // 验证URL格式
  try {
    new URL(imageUrl.value)
  } catch {
    ElMessage.error('请输入有效的URL地址')
    return
  }
  
  // 添加到文件列表
  uploadFileList.value.push({
    name: imageUrl.value.split('/').pop() || 'url-image.jpg',
    url: imageUrl.value,
    isUrl: true
  })
  
  imageUrl.value = ''
  ElMessage.success('URL图片已添加到上传列表')
}

// 提交上传
const submitUpload = async () => {
  if (uploadFileList.value.length === 0) {
    ElMessage.warning('请至少选择一张图片')
    return
  }
  
  if (resourceType.value === 'scenic' && !uploadImageType.value) {
    ElMessage.warning('请选择图片类型')
    return
  }
  
  try {
    ElMessage.info('正在上传图片...')
    
    const token = getToken()
    const file = uploadFileList.value[0] // 只取第一张图片
    
    if (!file.raw) {
      ElMessage.error('文件读取失败')
      return
    }
    
    // 使用 FormData 上传文件
    const formData = new FormData()
    formData.append('file', file.raw)
    
    let uploadUrl = ''
    let updateField = ''
    
    // 根据资源类型选择不同的上传策略
    if (resourceType.value === 'scenic') {
      // 景点：使用景点专用上传接口
      formData.append('imageType', uploadImageType.value)
      formData.append('sortOrder', '0')
      uploadUrl = `/api/admin/scenic/${currentUploadScenic.value.id}/images/upload`
    } else if (resourceType.value === 'activity') {
      // 活动：上传到OSS后更新活动封面
      uploadUrl = `/api/oss/upload`
      updateField = 'coverImage'
    } else if (resourceType.value === 'facility') {
      // 设施：上传到OSS后更新设施缩略图
      uploadUrl = `/api/oss/upload`
      updateField = 'thumbnail'
    }
    
    // 调用后端接口上传图片
    const response = await axios.post(uploadUrl, formData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    console.log('上传响应:', response.data)
    
    if (response.data.code === 1 || response.data.code === 200) {
      const imageUrl = response.data.data?.imageUrl || response.data.data?.url || response.data.data?.imageURL || response.data.data?.image_url || response.data.data
      
      // 如果是活动或设施，需要更新记录
      if (resourceType.value === 'activity' && updateField) {
        const { updateActivity } = await import('@/api/resource')
        const activity = activityResources.value.find(item => item.id === currentUploadScenic.value.id)
        if (activity) {
          await updateActivity(currentUploadScenic.value.id, {
            ...activity,
            [updateField]: imageUrl
          })
          activity.thumbnail = imageUrl
          ElMessage.success('活动封面图上传成功')
        }
      } else if (resourceType.value === 'facility' && updateField) {
        const { updateFacility } = await import('@/api/resource')
        const facility = facilityResources.value.find(item => item.id === currentUploadScenic.value.id)
        if (facility) {
          await updateFacility(currentUploadScenic.value.id, {
            ...facility,
            [updateField]: imageUrl
          })
          facility.thumbnail = imageUrl
          ElMessage.success('设施图片上传成功')
        }
      } else if (resourceType.value === 'scenic') {
        // 更新景点的缩略图（如果是封面图）
        if (uploadImageType.value === 'COVER') {
          const scenic = scenicResources.value.find(item => item.id === currentUploadScenic.value.id)
          if (scenic) {
            scenic.thumbnail = imageUrl
            scenic.updateTime = new Date().toISOString().split('T')[0]
            console.log('更新景点封面图:', {
              scenicId: scenic.id,
              newThumbnail: scenic.thumbnail
            })
          }
        }
        ElMessage.success('景点图片上传成功')
      }
      
      uploadDialogVisible.value = false
      uploadFileList.value = []
      uploadImageType.value = 'COVER'
      
      // 重新加载数据以确保同步
      setTimeout(() => {
        if (resourceType.value === 'scenic') loadScenicData()
        else if (resourceType.value === 'activity') loadActivities()
        else if (resourceType.value === 'facility') loadFacilities()
      }, 500)
    } else {
      ElMessage.error(response.data.msg || response.data.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传图片失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '上传失败，请重试')
  }
}

// 推广活动（活动管理）
const handlePromote = async (resource: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要推广活动"${resource.name}"吗？推广后该活动将在首页展示。`,
      '推广确认',
      {
        confirmButtonText: '确定推广',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 调用推广API
    const { promoteActivity } = await import('@/api/resource')
    await promoteActivity(resource.id)
    
    ElMessage.success(`活动"${resource.name}"已推广成功`)
    
    // 刷新活动列表
    await loadActivities()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('推广活动失败:', error)
      ElMessage.error('推广失败: ' + (error.message || '请求失败'))
    }
  }
}

// 设施维护（设施管理）
const handleMaintenance = (resource: any) => {
  ElMessageBox.confirm(
    `确定要将设施"${resource.name}"设为维护状态吗？维护期间该设施将无法使用。`,
    '维护确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    // 更新设施状态为维护中
    const facility = facilityResources.value.find(item => item.id === resource.id)
    if (facility) {
      facility.status = 'maintenance'
    }
    ElMessage.success(`设施"${resource.name}"已设为维护状态`)
    // 调用后端API更新设施状态
    try {
      const { updateFacility } = await import('@/api/resource')
      await updateFacility(resource.id, { status: 'maintenance' })
    } catch (apiError) {
      console.error('设施状态更新API调用失败:', apiError)
    }
  }).catch(() => {
    // 取消维护
  })
}

// 清空筛选条件
const clearFilters = () => {
  searchKeyword.value = ''
  selectedTags.value = []
}

// 景点资源数据（从后端加载）
const scenicResources = ref<any[]>([])

// 活动资源数据（从API加载）
const activityResources = ref<any[]>([])

// 设施资源数据（从API加载）
const facilityResources = ref<any[]>([])

// 加载真实景区数据（仅加载商家绑定的景区）
const loadScenicData = async () => {
  try {
    const token = getToken()
    
    // 1. 先获取商家资料，获取绑定的景区ID
    const userId = JSON.parse(localStorage.getItem('userInfo') || '{}').id
    if (!userId) {
      ElMessage.error('未找到用户信息，请重新登录')
      return
    }
    
    const profileResponse = await axios.get(`/api/merchant/profiles/by-user/${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    console.log('商家资料响应:', profileResponse.data)
    
    const merchantScenicId = profileResponse.data.data?.scenicId
    
    if (!merchantScenicId) {
      ElMessage.warning('您还未绑定景区，请先在"商家资料"页面选择要管理的景区')
      scenicResources.value = []
      return
    }
    
    // 2. 获取所有景区数据
    const response = await axios.get('/api/admin/scenic/list', {
      params: {
        page: 1,
        size: 100
      },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    console.log('加载景区数据响应:', response.data)
    
    if (response.data.code === 1 || response.data.code === 200) {
      const allScenicList = response.data.data.records || response.data.data || []
      
      // 3. 过滤：只保留商家绑定的景区
      const scenicList = allScenicList.filter((scenic: any) => scenic.id === merchantScenicId)
      
      // 转换为商户端需要的格式
      const scenicPromises = scenicList.map(async (scenic: any) => {
        // 解析标签
        let tags: string[] = []
        if (scenic.tags) {
          if (typeof scenic.tags === 'string') {
            try {
              tags = JSON.parse(scenic.tags)
            } catch {
              tags = scenic.tags.split(',').map((t: string) => t.trim())
            }
          } else if (Array.isArray(scenic.tags)) {
            tags = scenic.tags
          }
        }
        
        // 映射状态
        let status = 'active'
        if (scenic.status === 'ACTIVE') status = 'active'
        else if (scenic.status === 'INACTIVE') status = 'inactive'
        else if (scenic.status === 'MAINTENANCE') status = 'maintenance'
        
        // 获取景区的封面图 - 使用空字符串作为默认值，让UI显示默认图标
        let thumbnail = ''
        try {
          const imagesRes = await axios.get(`/api/admin/scenic/${scenic.id}/images`, {
            headers: { 'Authorization': `Bearer ${token}` }
          })
          
          if (imagesRes.data.code === 1 || imagesRes.data.code === 200) {
            const images = imagesRes.data.data || []
            // 查找封面图
            const coverImage = images.find((img: any) => img.imageType === 'COVER')
            if (coverImage) {
              thumbnail = coverImage.imageUrl || coverImage.imageURL || coverImage.image_url
            } else if (images.length > 0) {
              // 如果没有封面图，使用第一张图片
              thumbnail = images[0].imageUrl || images[0].imageURL || images[0].image_url
            }
          }
        } catch (error) {
          console.warn(`获取景区 ${scenic.id} 封面图失败:`, error)
        }
        
        return {
          id: scenic.id,  // 使用真实的景区ID
          name: scenic.name,
          thumbnail: thumbnail,
          description: scenic.description || '',
          tags: tags.length > 0 ? tags : ['自然风光'],
          status: status,
          visitCount: scenic.visitCount || 0,
          rating: scenic.rating || 4.5,
          createTime: scenic.createdAt ? scenic.createdAt.split(' ')[0] : '',
          updateTime: scenic.updatedAt ? scenic.updatedAt.split(' ')[0] : ''
        }
      })
      
      scenicResources.value = await Promise.all(scenicPromises)
      
      console.log('转换后的景区数据:', scenicResources.value)
      if (scenicResources.value.length > 0) {
        ElMessage.success(`已加载您管理的景区：${scenicResources.value[0].name}`)
      } else {
        ElMessage.warning('未找到您管理的景区数据')
      }
    } else {
      console.error('加载景区数据失败:', response.data)
      ElMessage.error('加载景区数据失败')
    }
  } catch (error: any) {
    console.error('加载景区数据异常:', error)
    ElMessage.error('加载景区数据失败: ' + (error.message || '请求失败'))
  }
}

// 加载活动数据
const loadActivities = async () => {
  try {
    const { getMerchantActivities } = await import('@/api/resource')
    const res: any = await getMerchantActivities({
      page: 1,
      size: 100
    })
    
    const data = res?.data || res
    const activities = data.activities || []
    
    // 转换数据格式
    activityResources.value = activities.map((activity: any) => ({
      id: activity.id, // 使用数字ID，不是字符串
      name: activity.name,
      thumbnail: activity.coverImage || '',
      description: activity.description,
      tags: JSON.parse(activity.tags || '[]'),
      status: activity.status,
      visitCount: activity.currentParticipants || 0,
      startTime: activity.startTime,
      endTime: activity.endTime,
      createTime: activity.createdAt,
      updateTime: activity.updatedAt,
      isPromoted: activity.isPromoted || false
    }))
    
    console.log('加载活动数据成功:', activityResources.value)
  } catch (error: any) {
    console.error('加载活动失败:', error)
    ElMessage.error('加载活动数据失败: ' + (error.message || '请求失败'))
  }
}

// 加载设施数据
const loadFacilities = async () => {
  try {
    const { getMerchantFacilities } = await import('@/api/resource')
    const res: any = await getMerchantFacilities({
      page: 1,
      size: 100
    })
    
    const data = res?.data || res
    const facilities = data.facilities || []
    
    // 转换数据格式
    facilityResources.value = facilities.map((facility: any) => ({
      id: facility.id, // 使用数字ID，不是字符串
      name: facility.name,
      thumbnail: facility.thumbnail || '',
      description: facility.description,
      tags: JSON.parse(facility.features || '[]'),
      status: facility.status,
      capacity: facility.capacity,
      openTime: facility.openingHours,
      createTime: facility.createdAt,
      updateTime: facility.updatedAt
    }))
    
    console.log('加载设施数据成功:', facilityResources.value)
  } catch (error: any) {
    console.error('加载设施失败:', error)
    ElMessage.error('加载设施数据失败: ' + (error.message || '请求失败'))
  }
}

// 子景点资源数据
const subSpotResources = ref<any[]>([])

// 子景点对话框
const subSpotDialogVisible = ref(false)
const subSpotFormRef = ref()

// 子景点表单
const subSpotForm = ref({
  name: '',
  description: '',
  imageUrl: '',
  tags: [] as string[],
  openingHours: '',
  sortOrder: 0
})

const subSpotRules = {
  name: [{ required: true, message: '请输入子景点名称', trigger: 'blur' }]
}

// 子景点筛选
const filteredSubSpots = computed(() => {
  if (!searchKeyword.value) return subSpotResources.value
  const kw = searchKeyword.value.toLowerCase()
  return subSpotResources.value.filter((s: any) =>
    s.name.toLowerCase().includes(kw) || (s.description || '').toLowerCase().includes(kw)
  )
})

// 加载子景点
const loadSubSpots = async () => {
  try {
    const { getMerchantSubSpots } = await import('@/api/resource')
    const res: any = await getMerchantSubSpots()
    const data = res?.data || res
    const list = Array.isArray(data) ? data : (data?.data || [])
    
    subSpotResources.value = list.map((item: any) => {
      let parsedTags: string[] = []
      if (item.tags) {
        if (typeof item.tags === 'string') {
          try { parsedTags = JSON.parse(item.tags) } catch { parsedTags = item.tags.split(',') }
        } else if (Array.isArray(item.tags)) {
          parsedTags = item.tags
        }
      }
      return { ...item, parsedTags }
    })
    
    console.log('加载子景点成功:', subSpotResources.value)
  } catch (error: any) {
    console.error('加载子景点失败:', error)
  }
}

// 添加子景点
const handleAddSubSpot = () => {
  isEditMode.value = false
  currentEditId.value = ''
  subSpotForm.value = { name: '', description: '', imageUrl: '', tags: [], openingHours: '', sortOrder: 0 }
  subSpotDialogVisible.value = true
}

// 编辑子景点
const handleEditSubSpot = (row: any) => {
  isEditMode.value = true
  currentEditId.value = row.id
  subSpotForm.value = {
    name: row.name || '',
    description: row.description || '',
    imageUrl: row.imageUrl || '',
    tags: row.parsedTags || [],
    openingHours: row.openingHours || '',
    sortOrder: row.sortOrder || 0
  }
  subSpotDialogVisible.value = true
}

// 删除子景点
const handleDeleteSubSpot = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除子景点"${row.name}"吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const { deleteSubSpot } = await import('@/api/resource')
    await deleteSubSpot(row.id)
    ElMessage.success('子景点删除成功')
    await loadSubSpots()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交子景点表单
const submitSubSpotForm = async () => {
  subSpotFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const payload = {
        name: subSpotForm.value.name,
        description: subSpotForm.value.description,
        imageUrl: subSpotForm.value.imageUrl,
        tags: subSpotForm.value.tags,
        openingHours: subSpotForm.value.openingHours,
        sortOrder: subSpotForm.value.sortOrder
      }
      
      if (isEditMode.value) {
        const { updateSubSpot } = await import('@/api/resource')
        await updateSubSpot(currentEditId.value, payload)
        ElMessage.success('子景点更新成功')
      } else {
        const { createSubSpot } = await import('@/api/resource')
        await createSubSpot(payload)
        ElMessage.success('子景点添加成功')
      }
      subSpotDialogVisible.value = false
      await loadSubSpots()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  })
}

// 子景点图片上传
const handleSubSpotImageUpload = async (options: any) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const token = getToken()
    const response = await axios.post('/api/oss/upload', formData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data.code === 1 || response.data.code === 200) {
      subSpotForm.value.imageUrl = response.data.data?.url || response.data.data
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(response.data.msg || '图片上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  }
}

// 组件挂载时加载真实数据
onMounted(() => {
  loadScenicData()
  loadActivities()
  loadFacilities()
  loadSubSpots()
})
</script>

<style scoped>
.resource-container {
  padding-bottom: 2rem;
}

:deep(.resource-tabs .el-tabs__header) {
  margin-bottom: 20px;
  border-bottom: 1px solid #EBEEF5;
}

:deep(.resource-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.resource-tabs .el-tabs__item) {
  color: #94A3B8;
  font-size: 16px;
  padding: 0 20px 10px;
}

:deep(.resource-tabs .el-tabs__item.is-active) {
  color: #2A9D8F;
}

:deep(.resource-tabs .el-tabs__active-bar) {
  background-color: #2A9D8F;
  height: 3px;
}

/* 上传组件样式 */
:deep(.upload-demo .el-upload) {
  width: 100%;
}

:deep(.upload-demo .el-upload-dragger) {
  background: #FAFAFA;
  border: 2px dashed #DCDFE6;
  transition: all 0.3s;
}

:deep(.upload-demo .el-upload-dragger:hover) {
  border-color: #2A9D8F;
  background: rgba(42, 157, 143, 0.05);
}

:deep(.upload-demo .el-icon--upload) {
  font-size: 48px;
  color: #2A9D8F;
  margin-bottom: 16px;
}

:deep(.upload-demo .el-upload__text) {
  color: #94A3B8;
  font-size: 14px;
}

:deep(.upload-demo .el-upload__text em) {
  color: #2A9D8F;
  font-style: normal;
  text-decoration: underline;
}

:deep(.upload-demo .el-upload__tip) {
  margin-top: 8px;
  font-size: 12px;
}

:deep(.upload-demo .el-upload-list) {
  margin-top: 10px;
}

:deep(.upload-demo .el-upload-list__item) {
  background: #FAFAFA;
  border: 1px solid #EBEEF5;
  transition: all 0.3s;
}

:deep(.upload-demo .el-upload-list__item:hover) {
  background: #F5F7FA;
  border-color: rgba(42, 157, 143, 0.3);
}

:deep(.upload-demo .el-upload-list__item-name) {
  color: #606266;
}
</style> 