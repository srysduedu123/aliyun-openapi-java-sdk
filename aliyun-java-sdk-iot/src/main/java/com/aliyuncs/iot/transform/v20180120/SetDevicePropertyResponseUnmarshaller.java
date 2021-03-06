/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyuncs.iot.transform.v20180120;

import com.aliyuncs.iot.model.v20180120.SetDevicePropertyResponse;
import com.aliyuncs.iot.model.v20180120.SetDevicePropertyResponse.Data;
import com.aliyuncs.transform.UnmarshallerContext;


public class SetDevicePropertyResponseUnmarshaller {

	public static SetDevicePropertyResponse unmarshall(SetDevicePropertyResponse setDevicePropertyResponse, UnmarshallerContext context) {
		
		setDevicePropertyResponse.setRequestId(context.stringValue("SetDevicePropertyResponse.RequestId"));
		setDevicePropertyResponse.setSuccess(context.booleanValue("SetDevicePropertyResponse.Success"));
		setDevicePropertyResponse.setErrorMessage(context.stringValue("SetDevicePropertyResponse.ErrorMessage"));
		setDevicePropertyResponse.setCode(context.stringValue("SetDevicePropertyResponse.Code"));

		Data data = new Data();
		data.setMessageId(context.stringValue("SetDevicePropertyResponse.Data.MessageId"));
		setDevicePropertyResponse.setData(data);
	 
	 	return setDevicePropertyResponse;
	}
}