package com.pbl6.fitme.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl6.fitme.R
import hoang.dqm.codebase.utils.singleClick

class ReviewBottomSheetFragment : BottomSheetDialogFragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.dialog_review, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
		val edtComment = view.findViewById<EditText>(R.id.edtComment)
		val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
		val tvOrderInfo = view.findViewById<TextView>(R.id.tvOrderInfo)

		// If caller passed order id, show it in order info
		val orderId = arguments?.getString("order_id")
		orderId?.let {
			tvOrderInfo.text = "Order #$it"
		}

		btnSubmit.singleClick {
			val rating = ratingBar.rating.toInt()
			val comment = edtComment.text.toString().trim()

			// TODO: send review to server via repository

			// Show 'done' dialog with rating
			val doneView = layoutInflater.inflate(R.layout.dialog_review_done, null)
			val doneDlg = android.app.AlertDialog.Builder(requireContext()).create()
			doneDlg.setView(doneView)
			val ratingDone = doneView.findViewById<RatingBar>(R.id.ratingBarDone)
			ratingDone.rating = rating.toFloat()

			doneDlg.setOnDismissListener {
				// dismiss bottom sheet when done dialog is closed
				dismiss()
			}

			doneDlg.show()
		}
	}
}