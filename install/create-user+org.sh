#!/bin/bash

#
# Copyright The Reshapr Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# User specific properties
USERNAME=jdoe
EMAIL=jdoe@example.com
PASSWORD=my-super-long-password-that-should-be-changed
FIRSTNAME=John
LASTNAME=Doe

# Server specific properties
SERVER_URL=http://localhost:5555
# The token below is the default one, but you should change it to your own token if you have changed it during installation.
SERVER_TOKEN=CzBuQ9B0i8qrUQe6WLiDLqR3gv4iCbxvjTJQP0z0CFGQbjgBHPZSusa9d1gZKwwjdoCsJ8ogRwRzc06GipJSjSDkFOy0BSOKvAa2EjU3As9I5UjgizTzxsJAVJIXtdo2xiXHhcry9KeJa0zRhDtGmm8WMujoXrlfj0ChlJKaHZiZsRthd4UHrWkKur9KySXpPFP21H4C0Cq6OgM1rJpvMZ7Jd2ZzeEcd5lKE4PlchHZBVEdu8jYzjQtU50fkOPoR

# Now create the user
echo "👤 Creating user '$USERNAME'..."
curl -XPOST $SERVER_URL/api/admin/users -H "Content-Type: application/json" -H "x-reshapr-api-key: $SERVER_TOKEN" \
  -d '{"username":"'$USERNAME'", "email":"'$EMAIL'", "password":"'$PASSWORD'", "firstName":"'$FIRSTNAME'", "lastName":"'$LASTNAME'"}'

# Now create the organization
echo ""
echo "🏢 Creating organization '$USERNAME'..."
curl -XPOST $SERVER_URL/api/admin/users/$USERNAME/organization -H "Content-Type: application/json" -H "x-reshapr-api-key: $SERVER_TOKEN" \
  -d '{"name":"'$USERNAME'", "description":"Organization for user '$USERNAME'"}'

# Now assign quotas to the organization
echo ""
echo "📊 Assigning quotas to organization '$USERNAME'..."
curl -XPOST $SERVER_URL/api/admin/quotas/organization/$USERNAME -H "Content-Type: application/json" -H "x-reshapr-api-key: $SERVER_TOKEN" \
  -d '[{"metric": "gateway-group.count", "enabled":true, "limit": 3}, {"metric": "gateway.count", "enabled":true, "limit": 3}, {"metric": "exposition.count", "enabled": true, "limit": 10}]'