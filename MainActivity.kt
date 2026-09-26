
package com.example.dominoscore

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

data class Match(val p1:String,val s1:Int,val p2:String,val s2:Int,val winner:String,val time:String)

class MainActivity : Activity() {
    private lateinit var p1Name: EditText
    private lateinit var p2Name: EditText
    private lateinit var s1: TextView
    private lateinit var s2: TextView
    private lateinit var input: EditText
    private lateinit var root: FrameLayout
    private val prefs by lazy { getSharedPreferences("domino", MODE_PRIVATE) }
    private val history = mutableListOf<Match>()
    private var score1=0; private var score2=0
    private var effects=true

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor=Color.rgb(7,21,28)
        window.navigationBarColor=Color.rgb(7,21,28)
        loadHistory()
        build()
    }

    private fun glass(bg:Int=0x183E5963, stroke:Int=0x557EA0AA, radius:Float=28f): GradientDrawable =
        GradientDrawable().apply { setColor(bg); setStroke(2,stroke); cornerRadius=radius }

    private fun tv(text:String,size:Float=18f,bold:Boolean=false): TextView =
        TextView(this).apply {
            this.text=text; textSize=size; setTextColor(Color.WHITE)
            gravity=Gravity.CENTER; if(bold) typeface=Typeface.DEFAULT_BOLD
            setPadding(12,8,12,8)
        }

    private fun button(text:String, onClick:()->Unit): TextView =
        tv(text,16f,true).apply {
            background=glass(0x253F6570,0x667FA6B2,24f); setPadding(20,16,20,16)
            setOnClickListener { onClick() }
        }

    private fun build(){
        root=FrameLayout(this)
        val scroll=ScrollView(this)
        val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(22,20,22,30)}
        root.addView(scroll,FrameLayout.LayoutParams(-1,-1)); scroll.addView(col)

        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL; background=glass(0x26405D68,0x6686B2BF,32f);setPadding(14,12,14,12)}
        val title=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(0,100,1f)}
        title.addView(tv("Domino Score",26f,true)); title.addView(tv("تسجيل نقاط الدومنة",14f))
        header.addView(title)
        val settings=button("⚙"){settingsDialog()}
        header.addView(settings,LinearLayout.LayoutParams(60,70))
        col.addView(header,LinearLayout.LayoutParams(-1,110))

        col.addView(space(14))
        val names=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        p1Name=edit("الطرف الأول"); p2Name=edit("الطرف الثاني")
        names.addView(p1Name,LinearLayout.LayoutParams(0,60,1f)); names.addView(spaceH(10)); names.addView(p2Name,LinearLayout.LayoutParams(0,60,1f))
        col.addView(names)

        col.addView(space(14))
        val scores=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        val c1=playerCard(true); val c2=playerCard(false)
        scores.addView(c1,LinearLayout.LayoutParams(0,290,1f)); scores.addView(spaceH(12)); scores.addView(c2,LinearLayout.LayoutParams(0,290,1f))
        col.addView(scores)

        col.addView(space(14))
        val inputBox=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=glass(0x203E5C67,0x6682AEB8,28f);setPadding(12,10,12,10)}
        input=EditText(this).apply{
            hint="النقاط"; textSize=22f; gravity=Gravity.CENTER; inputType=2; setTextColor(Color.WHITE); setHintTextColor(0x99FFFFFF)
            background=glass(0x1FFFFFFF.toInt(),0x557EA0AA,22f)
        }
        inputBox.addView(input,LinearLayout.LayoutParams(0,65,1f))
        inputBox.addView(button("+ للطرف الأول"){addPoints(true)})
        inputBox.addView(button("+ للطرف الثاني"){addPoints(false)})
        col.addView(inputBox)

        col.addView(space(14))
        val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        actions.addView(button("RESET"){resetDialog()},LinearLayout.LayoutParams(0,62,1f))
        actions.addView(spaceH(12))
        actions.addView(button("جولة جديدة"){newRound()},LinearLayout.LayoutParams(0,62,1f))
        col.addView(actions)

        col.addView(space(18))
        col.addView(tv("آخر الجولات",22f,true).apply{gravity=Gravity.RIGHT})
        col.addView(space(8))
        renderHistory(col)

        setContentView(root)
        refresh()
    }

    private fun playerCard(first:Boolean): LinearLayout {
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; background=glass(if(first)0x304D8397 else 0x304F527F,0x668EC8D8,32f)
        }
        val name=if(first) p1Name else p2Name
        val score=if(first) s1 else s2
        if(first) s1=tv("0",58f,true) else s2=tv("0",58f,true)
        box.addView(name,LinearLayout.LayoutParams(-1,55))
        box.addView(score,LinearLayout.LayoutParams(-1,110))
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER}
        row.addView(button("−"){change(first,-1)})
        row.addView(button("＋"){change(first,1)})
        box.addView(row)
        return box
    }

    private fun edit(hint:String)=EditText(this).apply{
        this.hint=hint;textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setHintTextColor(0x99FFFFFF)
        background=glass(0x203F5C66,0x557FA6B0,24f);setPadding(10,0,10,0)
    }

    private fun change(first:Boolean,delta:Int){
        if(first) score1=(score1+delta).coerceAtLeast(0).coerceAtMost(151) else score2=(score2+delta).coerceAtLeast(0).coerceAtMost(151)
        refresh()
    }

    private fun addPoints(first:Boolean){
        val n=input.text.toString().toIntOrNull()?:return
        if(n<=0)return
        val old=if(first)score1 else score2
        val next=(old+n).coerceAtMost(151)
        if(first)score1=next else score2=next
        input.setText("")
        if(n==5 && effects) toastGlass("طارت الخمسة 🎉")
        refresh()
        if(score1>=151 || score2>=151) finishRound()
    }

    private fun refresh(){ if(::s1.isInitialized)s1.text=score1.toString(); if(::s2.isInitialized)s2.text=score2.toString() }

    private fun finishRound(){
        val a=p1Name.text.toString().ifBlank{"الطرف الأول"}; val b=p2Name.text.toString().ifBlank{"الطرف الثاني"}
        val winner=if(score1>=151)a else b
        val t=SimpleDateFormat("yyyy/MM/dd - HH:mm",Locale.getDefault()).format(Date())
        history.add(0,Match(a,score1,b,score2,winner,t)); while(history.size>10)history.removeAt(history.lastIndex)
        saveHistory()
        val msg=if((if(score1>=151)score2 else score1)==0) "روح نيّج أحسلك من الصفر 😂" else "🎉 مبروك!\nفاز $winner"
        toastGlass(msg)
        renderAllHistory()
    }

    private fun renderAllHistory(){ build() }

    private fun renderHistory(col:LinearLayout){
        if(history.isEmpty()){col.addView(tv("لا توجد جولات محفوظة بعد",15f).apply{setPadding(0,25,0,25)});return}
        history.forEach{
            val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=glass(0x193E5963,0x557EA0AA,24f);setPadding(14,12,14,12)}
            card.addView(tv("${it.p1}  ${it.s1}   —   ${it.s2}  ${it.p2}",18f,true))
            card.addView(tv("الفائز: ${it.winner}\n${it.time}",13f))
            col.addView(card,LinearLayout.LayoutParams(-1,95));col.addView(space(8))
        }
    }

    private fun resetDialog(){
        AlertDialog.Builder(this).setTitle("تصفير النقاط").setMessage("هل تريد تصفير النقاط؟")
            .setNegativeButton("إلغاء",null).setPositiveButton("نعم، تصفير"){_,_->score1=0;score2=0;refresh()}.show()
    }
    private fun newRound(){score1=0;score2=0;p1Name.setText("");p2Name.setText("");refresh()}
    private fun settingsDialog(){
        val sw=Switch(this).apply{text="المؤثرات الاحتفالية";isChecked=effects}
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,10,30,10);addView(sw)}
        AlertDialog.Builder(this).setTitle("الإعدادات").setView(box).setPositiveButton("حفظ"){_,_->effects=sw.isChecked}.setNegativeButton("إلغاء",null).show()
    }
    private fun toastGlass(msg:String){
        val d=Dialog(this); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val t=tv(msg,22f,true).apply{background=glass(0xD92B5261.toInt(),0x889DD9E5.toInt(),34f);setPadding(35,35,35,35)}
        d.setContentView(t);d.window?.setDimAmount(0.15f);d.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        d.show(); t.postDelayed({if(d.isShowing)d.dismiss()},5000)
    }
    private fun space(h:Int)=Space(this).apply{layoutParams=LinearLayout.LayoutParams(1,h)}
    private fun spaceH(w:Int)=Space(this).apply{layoutParams=LinearLayout.LayoutParams(w,1)}
    private fun saveHistory(){
        val s=history.joinToString("\n"){listOf(it.p1,it.s1,it.p2,it.s2,it.winner,it.time).joinToString("\t")}
        prefs.edit().putString("history",s).apply()
    }
    private fun loadHistory(){
        prefs.getString("history","")?.lines()?.filter{it.isNotBlank()}?.forEach{p->
            val x=p.split("\t");if(x.size>=6)history.add(Match(x[0],x[1].toIntOrNull()?:0,x[2],x[3].toIntOrNull()?:0,x[4],x[5]))
        }
    }
}
