import korolev.Korolev.EventFactory
import korolev.{Event, KorolevServer, Shtml}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
object Example extends App with Shtml {

  import korolev.EventResult._

  case class Todo(text: String, done: Boolean)

  case class State(todos: Vector[Todo] = (0 to 10).toVector map {
    i => Todo(s"This is TODO #$i", done = false)
  })

  KorolevServer[State](
    initialState = State(),
    initRender = { access =>

      // Handler to input
      val inputId = access.id()

      // Generate actions when clicking checkboxes
      val todoClick: EventFactory[(Int, Todo)] =
        access.event("click", Event.AtTarget) { case (i, todo) =>
          immediateTransition { case state =>
            val updated = state.todos.updated(i, state.todos(i).copy(done = !todo.done))
            state.copy(todos = updated)
          }
        }

      // Generate AddTodo action when 'Add' button clicked
      val addTodoClick: EventFactory[Unit] =
        access.event("click") { _ =>
          deferredTransition {
            inputId.get[String]('value) map { value =>
              val todo = Todo(value, done = false)
              transition {
                case state =>
                  state.copy(todos = state.todos :+ todo)
              }
            }
          }
        }

      // Create a DOM using state
      { case state =>
        'div(
          'div("Super TODO tracker"),
          'div('style /= "height: 250px; overflow-y: scroll",
            (state.todos zipWithIndex) map {
              case (todo, i) =>
                'div(
                  'input(
                    'type /= "checkbox",
                    'checked := todo.done,
                    todoClick(i, todo)
                  ),
                  if (!todo.done) 'span(todo.text)
                  else 'strike(todo.text)
                )
            }
          ),
          'div(
            'input(
              inputId,
              'type /= "text",
              'placeholder /= "What should be done?"
            ),
            'button(
              "Add todo",
              addTodoClick(())
            )
          )
        )
      }
    }
  )
}
