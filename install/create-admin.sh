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
USERNAME=admin
EMAIL=reshapr@example.com
PASSWORD=password
FIRSTNAME=Reshapr
LASTNAME=Admin

# Server specific properties
SERVER_URL=http://localhost:5555
# The token below is the default one, but you should change it to your own token if you have changed it during installation.
SERVER_TOKEN=CzBuQ9B0i8qrUQe6WLiDLqR3gv4iCbxvjTJQP0z0CFGQbjgBHPZSusa9d1gZKwwjdoCsJ8ogRwRzc06GipJSjSDkFOy0BSOKvAa2EjU3As9I5UjgizTzxsJAVJIXtdo2xiXHhcry9KeJa0zRhDtGmm8WMujoXrlfj0ChlJKaHZiZsRthd4UHrWkKur9KySXpPFP21H4C0Cq6OgM1rJpvMZ7Jd2ZzeEcd5lKE4PlchHZBVEdu8jYzjQtU50fkOPoR

# Now create the admin user
echo "👤 Creating user '$USERNAME'..."
curl -XPOST $SERVER_URL/api/admin/users -H "Content-Type: application/json" -H "x-reshapr-api-key: $SERVER_TOKEN" \
  -d '{"username":"'$USERNAME'", "email":"'$EMAIL'", "password":"'$PASSWORD'", "firstname":"'$FIRSTNAME'", "lastname":"'$LASTNAME'"}'

# Now set the user as owner of reshapr organization
echo ""
echo "🏢 Set '$USERNAME' as owner of reshapr organization..."
curl -XPUT $SERVER_URL/api/admin/users/$USERNAME/organization/reshapr/owner -H "x-reshapr-api-key: $SERVER_TOKEN"
