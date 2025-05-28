package com.anmi.camera.uvcplay.model

import java.io.Serializable

/**[
{
"id": 0,
"case_id": "string",
"case_debtor": "string",
"visit_address": "string",
"debtor_image": "string",
"created_by": "string",
"updated_by": "string",
"create_time": "2025-05-25T14:35:34.162Z",
"update_time": "2025-05-25T14:35:34.162Z"
}
]
 */
data class CaseModel(
    val case_debtor: String,
    val case_id: String,
    val visit_address: String,
    val debtor_image: String
) : Serializable{
    override fun toString(): String {
        return "CaseModel(case_debtor='$case_debtor', case_id='$case_id', visit_address='$visit_address', debtor_image='$debtor_image')"
    }
}

