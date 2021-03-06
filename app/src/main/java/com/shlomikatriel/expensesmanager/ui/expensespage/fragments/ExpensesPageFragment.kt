package com.shlomikatriel.expensesmanager.ui.expensespage.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.shlomikatriel.expensesmanager.ExpensesManagerApp
import com.shlomikatriel.expensesmanager.R
import com.shlomikatriel.expensesmanager.database.Expense
import com.shlomikatriel.expensesmanager.databinding.ExpensesPageFragmentBinding
import com.shlomikatriel.expensesmanager.extensions.safeNavigate
import com.shlomikatriel.expensesmanager.logs.Logger
import com.shlomikatriel.expensesmanager.ui.expenses.fragments.ExpensesMainFragmentDirections
import com.shlomikatriel.expensesmanager.ui.expensespage.mvi.*
import com.shlomikatriel.expensesmanager.ui.expensespage.recyclers.ExpensesPageRecyclerAdapter
import com.shlomikatriel.expensesmanager.utils.AnimationFactory
import java.text.NumberFormat
import javax.inject.Inject
import javax.inject.Named

class ExpensesPageFragment : Fragment() {

    @Inject
    lateinit var appContext: Context

    @Inject
    lateinit var animationFactory: AnimationFactory

    @Inject
    lateinit var currencyFormat: NumberFormat

    @Inject
    @Named("integer")
    lateinit var currencyIntegerFormat: NumberFormat

    private val args: ExpensesPageFragmentArgs by navArgs()

    private lateinit var binding: ExpensesPageFragmentBinding

    private lateinit var model: ExpensesPageViewModel

    private lateinit var expensesRecyclerAdapter: ExpensesPageRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (requireContext().applicationContext as ExpensesManagerApp).appComponent.inject(this)

        binding = DataBindingUtil.inflate<ExpensesPageFragmentBinding>(
            inflater,
            R.layout.expenses_page_fragment,
            container,
            false
        ).apply {
            fragment = this@ExpensesPageFragment
            currencyFormat = currencyIntegerFormat
        }

        initializeViewModel()

        configureRecycler()

        return binding.root
    }

    private fun configureRecycler() = binding.expensesRecycler.apply {
        layoutManager = LinearLayoutManager(requireContext())
        expensesRecyclerAdapter = ExpensesPageRecyclerAdapter(
            requireContext(),
            findNavController(),
            currencyFormat
        )
        adapter = expensesRecyclerAdapter
    }

    fun onCheckedChanged() {
        val selectedChips = getSelectedChips()
        Logger.i("Selected chips changed [selectedChips=$selectedChips]")
        model.postEvent(ExpensesPageEvent.SelectedChipsChangedEvent(selectedChips))
    }

    private fun initializeViewModel() {
        model = ViewModelProvider(
            this,
            ExpensesPageViewModelFactory(appContext, args.month, args.year)
        ).get(args.pagePosition.toString(), ExpensesPageViewModel::class.java)
            .apply {
                postEvent(ExpensesPageEvent.InitializeEvent)
                getViewState().observe(viewLifecycleOwner, Observer { render(it) })
            }
    }

    private fun render(viewState: ExpensesPageViewState) {
        val filteredExpenses = filterExpensesUsingChips(viewState.expenses, viewState.selectedChips)
        expensesRecyclerAdapter.updateData(filteredExpenses)
        val sum = filteredExpenses.sumByDouble { it.amount.toDouble() }
        binding.sum = currencyFormat.format(sum)

        viewState.balance?.let {
            binding.balance.text = currencyFormat.format(viewState.balance)
            @ColorRes val color = if (viewState.balance >= 0) R.color.green else R.color.red
            binding.balance.setTextColor(ContextCompat.getColor(appContext, color))
        }
    }

    private fun filterExpensesUsingChips(
        expenses: ArrayList<Expense>,
        selectedChips: List<Chip>
    ): ArrayList<Expense> {
        val newExpenses = expenses.filter { Chip.shouldShow(it, selectedChips) }.toTypedArray()
        return arrayListOf(*newExpenses)
    }

    private fun getSelectedChips() = binding.chipGroup.checkedChipIds
        .mapNotNull { binding.chipGroup.findViewById<View>(it).tag as Chip? }
        .toList()


    fun addExpenseClicked(view: View) {
        Logger.i("Add expense button clicked")
        view.startAnimation(animationFactory.createPopAnimation())
        findNavController().safeNavigate(
            ExpensesMainFragmentDirections.openAddExpenseDialog(args.month, args.year)
        )
    }
}