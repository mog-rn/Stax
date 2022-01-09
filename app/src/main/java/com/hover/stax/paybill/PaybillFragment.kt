package com.hover.stax.paybill

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.hover.sdk.actions.HoverAction
import com.hover.stax.R
import com.hover.stax.channels.Channel
import com.hover.stax.channels.ChannelsViewModel
import com.hover.stax.databinding.FragmentPaybillBinding
import com.hover.stax.utils.Constants
import com.hover.stax.views.AbstractStatefulInput
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class PaybillFragment : Fragment() {

    private var _binding: FragmentPaybillBinding? = null
    private val binding get() = _binding!!

    private val channelsViewModel: ChannelsViewModel by viewModel()
    private val paybillViewModel: PaybillViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaybillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelsViewModel.setType(HoverAction.C2B)

        arguments?.getBoolean(UPDATE_BUSINESS_NO, false)?.let { binding.billDetailsLayout.businessNoInput.setText(paybillViewModel.businessNumber.value) }

        initListeners()
        startObservers()
        setWatchers()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        binding.saveBillLayout.saveBill.setOnCheckedChangeListener { _, isChecked ->
            binding.saveBillLayout.saveBillCard.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.billDetailsLayout.businessNoInput.editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                channelsViewModel.activeAccount.value?.id?.let {
                    findNavController().navigate(R.id.action_paybillFragment_to_paybillListFragment, bundleOf(Constants.ACCOUNT_ID to it))
                } ?: Timber.e("Active account not set")
                true
            } else false
        }

        binding.continueBtn.setOnClickListener {
            if (validates()) {
                if (binding.saveBillLayout.saveBill.isChecked) {
                    Timber.e("Saving bill")
                    paybillViewModel.savePaybill(channelsViewModel.activeAccount.value, binding.saveBillLayout.amountCheckBox.isChecked)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startObservers() {
        paybillViewModel.selectedPaybill.observe(viewLifecycleOwner) {
            binding.billDetailsLayout.businessNoInput.setText(it.name)
        }

        with(channelsViewModel) {
            binding.billDetailsLayout.accountDropdown.apply {
                setListener(this@with)
                setObservers(this@with, viewLifecycleOwner)
            }

            setupActionDropdownObservers(this, viewLifecycleOwner)

            accounts.observe(viewLifecycleOwner) {
                if (it.isEmpty())
                    binding.billDetailsLayout.accountDropdown.autoCompleteTextView.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN)
                            findNavController().navigate(R.id.action_paybillFragment_to_accountsFragment)
                        true
                    }
            }
        }
    }

    private fun setupActionDropdownObservers(viewModel: ChannelsViewModel, lifecycleOwner: LifecycleOwner) {

        val activeChannelObserver = object : Observer<Channel> {
            override fun onChanged(t: Channel?) {
                Timber.i("Got new active channel: $t ${t?.countryAlpha2}")
            }
        }

        val actionsObserver = object : Observer<List<HoverAction>> {
            override fun onChanged(t: List<HoverAction>?) {
                Timber.i("Got new actions: %s", t?.size)
            }
        }

        viewModel.activeChannel.observe(lifecycleOwner, activeChannelObserver)
        viewModel.channelActions.observe(lifecycleOwner, actionsObserver)
    }

    private fun setWatchers() {
        with(binding.billDetailsLayout) {
            businessNoInput.addTextChangedListener(businessNoWatcher)
            accountNoInput.addTextChangedListener(accountNoWatcher)
            amountInput.addTextChangedListener(amountWatcher)
        }

        binding.saveBillLayout.billNameInput.addTextChangedListener(nicknameWatcher)
    }

    private val amountWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            paybillViewModel.setAmount(charSequence.toString().replace(",".toRegex(), ""))
        }
    }

    private val businessNoWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            paybillViewModel.setBusinessNumber(charSequence.toString())
        }
    }

    private val accountNoWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            paybillViewModel.setAccountNumber(charSequence.toString().replace(",".toRegex(), ""))
        }
    }

    private val nicknameWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            paybillViewModel.setNickname(charSequence.toString())
        }
    }

    private fun validates(): Boolean {
        val businessNoError = paybillViewModel.businessNoError()
        val accountNoError = paybillViewModel.accountNoError()
        val amountError = paybillViewModel.amountError()
        val nickNameError = paybillViewModel.nameError()

        with(binding.billDetailsLayout) {
            businessNoInput.setState(businessNoError, if (businessNoError == null) AbstractStatefulInput.SUCCESS else AbstractStatefulInput.NONE)
            accountNoInput.setState(accountNoError, if (accountNoError == null) AbstractStatefulInput.SUCCESS else AbstractStatefulInput.NONE)
            amountInput.setState(amountError, if (amountError == null) AbstractStatefulInput.SUCCESS else AbstractStatefulInput.NONE)
        }

        if (binding.saveBillLayout.saveBill.isChecked)
            binding.saveBillLayout.billNameInput.setState(nickNameError, if (nickNameError == null) AbstractStatefulInput.SUCCESS else AbstractStatefulInput.NONE)

        return businessNoError == null && accountNoError == null && amountError == null && nickNameError == null
    }

    override fun onResume() {
        super.onResume()
        //sometimes when navigating back from another fragment, the labels get all messed up
        with(binding.billDetailsLayout) {
            accountDropdown.setHint(getString(R.string.account_label))
            businessNoInput.setHint(getString(R.string.business_number_label))
            accountNoInput.setHint(getString(R.string.account_number_label))
            amountInput.setHint(getString(R.string.transfer_amount_label))

            businessNoInput.binding.inputLayout.apply {
                setEndIconDrawable(R.drawable.ic_twotone_chevron_right_24)
                setEndIconTintMode(PorterDuff.Mode.SRC_IN)
                setEndIconTintList(ColorStateList.valueOf(Color.WHITE))
            }
        }
    }

    companion object {
        const val UPDATE_BUSINESS_NO: String = "update_business_no"
    }
}
