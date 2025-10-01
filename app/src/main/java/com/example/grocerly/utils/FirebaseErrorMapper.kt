package com.example.grocerly.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object FirebaseErrorMapper {




    fun getUserMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthException -> getAuthErrorMessage(exception)
            is FirebaseFirestoreException -> getFirestoreErrorMessage(exception)
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            else -> "Something went wrong. Please try again later."
        }
    }

    private fun getAuthErrorMessage(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered."
            "ERROR_USER_NOT_FOUND" -> "No user found with this email."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password. Try again."
            "ERROR_USER_DISABLED" -> "This user account has been disabled."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait and try again."
            "ERROR_OPERATION_NOT_ALLOWED" -> "This operation is not allowed. Contact support."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "This email is already linked to another sign-in method."
            "ERROR_INVALID_CREDENTIAL" -> "Invalid credentials. Please try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your internet connection."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "This action requires recent login. Please sign in again."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "This credential is already associated with another user."
            "ERROR_INVALID_VERIFICATION_CODE" -> "The verification code is invalid or expired."
            "ERROR_INVALID_VERIFICATION_ID" -> "The verification ID is invalid."
            "ERROR_MISSING_EMAIL" -> "Please enter your email."
            "ERROR_MISSING_PASSWORD" -> "Please enter your password."
            "ERROR_MISSING_PHONE_NUMBER" -> "Phone number is required."
            "ERROR_SESSION_EXPIRED" -> "Your session has expired. Please try again."
            "ERROR_INVALID_PHONE_NUMBER" -> "Invalid phone number format."
            "ERROR_USER_TOKEN_EXPIRED" -> "Session token has expired. Please log in again."
            "ERROR_INVALID_USER_TOKEN" -> "Invalid session token. Try logging in again."
            "ERROR_USER_MISMATCH" -> "The provided credentials do not match this user."
            "ERROR_PROVIDER_ALREADY_LINKED" -> "This provider is already linked to your account."
            "ERROR_NO_SUCH_PROVIDER" -> "This provider is not linked to your account."
            "ERROR_EMAIL_CHANGE_NEEDS_VERIFICATION" -> "Email change requires verification. Please check your inbox."
            "ERROR_UNAUTHORIZED_DOMAIN" -> "This domain is not authorized for authentication."
            "ERROR_INVALID_DYNAMIC_LINK_DOMAIN" -> "Invalid dynamic link domain. Contact support."
            "ERROR_INVALID_CONTINUE_URI" -> "Invalid continue URI. Please try again."
            "ERROR_ADMIN_RESTRICTED_OPERATION" -> "This action is restricted by your administrator."
            "ERROR_APP_NOT_AUTHORIZED" -> "This app is not authorized to access Firebase Authentication."
            "ERROR_EXPIRED_ACTION_CODE" -> "The action code has expired. Please request a new one."
            "ERROR_INVALID_ACTION_CODE" -> "The action code is invalid or has already been used."
            else -> "Authentication failed. Please try again."
        }
    }

    private fun getFirestoreErrorMessage(exception: FirebaseFirestoreException): String {
        return when (exception.code) {
            FirebaseFirestoreException.Code.CANCELLED ->
                "The request was cancelled. Please try again."

            FirebaseFirestoreException.Code.OK ->
                "Operation completed successfully."

            FirebaseFirestoreException.Code.UNKNOWN ->
                "An unknown error occurred. Please try again."

            FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                "An invalid argument was provided. Check your input."

            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "The operation timed out. Please check your connection and try again."

            FirebaseFirestoreException.Code.NOT_FOUND ->
                "The requested document was not found."

            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "The document already exists."

            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don’t have permission to perform this action."

            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Resource limits exceeded. Try again later."

            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Operation failed due to a precondition check. The data may be out of sync."

            FirebaseFirestoreException.Code.ABORTED ->
                "The operation was aborted, possibly due to a concurrency issue. Please retry."

            FirebaseFirestoreException.Code.OUT_OF_RANGE ->
                "A value was out of allowed range. Please check your input."

            FirebaseFirestoreException.Code.UNIMPLEMENTED ->
                "This operation is not supported by Firestore."

            FirebaseFirestoreException.Code.INTERNAL ->
                "An internal server error occurred. Try again later."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore is currently unavailable. Please try again later."

            FirebaseFirestoreException.Code.DATA_LOSS ->
                "Data corruption or loss detected. Operation failed."

            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication is required to access this resource. Please log in."
        }
    }

}